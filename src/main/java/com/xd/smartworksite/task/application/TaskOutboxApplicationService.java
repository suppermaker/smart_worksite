package com.xd.smartworksite.task.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.redis.RedisKeys;
import com.xd.smartworksite.common.redis.RedisQueueService;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskOutboxEvent;
import com.xd.smartworksite.task.domain.TaskOutboxStatus;
import com.xd.smartworksite.task.dto.TaskQueueMessage;
import com.xd.smartworksite.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TaskOutboxApplicationService {
    public static final String EVENT_TASK_QUEUED = "TASK_QUEUED";
    public static final String TASK_QUEUE_NAME = "task:pending";
    private static final int MAX_DELIVERY_COUNT = 5;
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final Logger log = LoggerFactory.getLogger(TaskOutboxApplicationService.class);

    private final TaskRepository taskRepository;
    private final RedisQueueService redisQueueService;
    private final ObjectMapper objectMapper;

    public TaskOutboxApplicationService(TaskRepository taskRepository,
                                        RedisQueueService redisQueueService,
                                        ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.redisQueueService = redisQueueService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueueTask(GenerateTask task, String reason) {
        if (task == null || task.getId() == null || task.getProjectId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "task id and project id are required for outbox enqueue");
        }
        TaskOutboxEvent event = new TaskOutboxEvent();
        event.setTaskId(task.getId());
        event.setProjectId(task.getProjectId());
        event.setEventType(EVENT_TASK_QUEUED);
        event.setPayload(toJson(Map.of(
                "taskId", task.getId(),
                "projectId", task.getProjectId(),
                "taskType", task.getTaskType() == null ? "" : task.getTaskType(),
                "bizType", task.getBizType() == null ? "" : task.getBizType(),
                "bizId", task.getBizId() == null ? 0L : task.getBizId(),
                "reason", reason == null ? "" : reason
        )));
        event.setStatus(TaskOutboxStatus.PENDING.name());
        event.setNextDeliveryAt(LocalDateTime.now());
        int inserted = taskRepository.insertOutboxEvent(event);
        if (inserted == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "task outbox event insert failed");
        }
        if (event.getId() == null) {
            TaskOutboxEvent persisted = taskRepository.findOutboxEvent(task.getId(), EVENT_TASK_QUEUED)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "task outbox event is not readable"));
            event.setId(persisted.getId());
        }
    }

    public int deliverDueEvents(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<TaskOutboxEvent> events = taskRepository.findDueOutboxEvents(safeLimit);
        int delivered = 0;
        for (TaskOutboxEvent event : events) {
            if (deliverOne(event)) {
                delivered++;
            }
        }
        return delivered;
    }

    private boolean deliverOne(TaskOutboxEvent event) {
        try {
            redisQueueService.push(RedisKeys.queue(TASK_QUEUE_NAME), toJson(toQueueMessage(event)));
            int updated = taskRepository.markOutboxDelivered(event.getId());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "task outbox state changed before delivered");
            }
            log.info("task outbox delivered, eventId={}, taskId={}, eventType={}",
                    event.getId(), event.getTaskId(), event.getEventType());
            return true;
        } catch (RuntimeException ex) {
            int deliveryCount = event.getDeliveryCount() == null ? 0 : event.getDeliveryCount();
            String nextStatus = deliveryCount + 1 >= MAX_DELIVERY_COUNT
                    ? TaskOutboxStatus.FAILED.name()
                    : TaskOutboxStatus.PENDING.name();
            long delaySeconds = retryDelaySeconds(deliveryCount);
            int updated = taskRepository.markOutboxFailed(event.getId(), nextStatus, truncate(ex.getMessage()), delaySeconds);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "task outbox failure state update failed");
            }
            log.error("task outbox delivery failed, eventId={}, taskId={}, nextStatus={}, delaySeconds={}",
                    event.getId(), event.getTaskId(), nextStatus, delaySeconds, ex);
            return false;
        }
    }

    private TaskQueueMessage toQueueMessage(TaskOutboxEvent event) {
        TaskQueueMessage message = new TaskQueueMessage();
        message.setEventId(event.getId());
        message.setTaskId(event.getTaskId());
        message.setProjectId(event.getProjectId());
        message.setEventType(event.getEventType());
        message.setPayload(event.getPayload());
        return message;
    }

    private long retryDelaySeconds(int deliveryCount) {
        return Math.min(300L, 5L * (1L << Math.min(deliveryCount, 5)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "task outbox json serialization failed");
        }
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "task outbox delivery failed";
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
