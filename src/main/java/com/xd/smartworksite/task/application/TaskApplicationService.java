package com.xd.smartworksite.task.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.redis.RedisKeys;
import com.xd.smartworksite.common.redis.RedisQueueService;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskStageCode;
import com.xd.smartworksite.task.domain.TaskStageLog;
import com.xd.smartworksite.task.domain.TaskStatus;
import com.xd.smartworksite.task.dto.TaskResponse;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import com.xd.smartworksite.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskApplicationService {

    private static final String TASK_QUEUE_NAME = "tasks";

    private final TaskRepository taskRepository;
    private final RedisQueueService redisQueueService;

    public TaskApplicationService(TaskRepository taskRepository, RedisQueueService redisQueueService) {
        this.taskRepository = taskRepository;
        this.redisQueueService = redisQueueService;
    }

    public TaskResponse getTask(Long projectId, Long taskId) {
        return toTaskResponse(loadTask(projectId, taskId));
    }

    public List<TaskStageLogResponse> getStageLogs(Long projectId, Long taskId) {
        GenerateTask task = loadTask(projectId, taskId);
        return taskRepository.findStageLogsByTaskId(task.getId()).stream()
                .map(this::toStageLogResponse)
                .toList();
    }

    @Transactional
    public TaskResponse retryTask(Long projectId, Long taskId) {
        GenerateTask task = loadTask(projectId, taskId);
        task.ensureRetryAllowed();
        task.transitionTo(TaskStatus.RETRYING);
        if (!taskRepository.retry(task)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Task status changed, retry rejected");
        }
        redisQueueService.push(RedisKeys.queue(TASK_QUEUE_NAME), String.valueOf(task.getId()));
        return getTask(projectId, taskId);
    }

    @Transactional
    public TaskResponse cancelTask(Long projectId, Long taskId) {
        GenerateTask task = loadTask(projectId, taskId);
        task.ensureCancelable();
        task.transitionTo(TaskStatus.CANCELED);
        if (!taskRepository.updateStatus(task, TaskStatus.CANCELED, task.getCurrentStage(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Task status changed, cancel rejected");
        }
        return getTask(projectId, taskId);
    }

    @Transactional
    public void recordStage(TaskStageLog stageLog) {
        GenerateTask task = loadTask(stageLog.getTaskId());
        if (!task.getProjectId().equals(stageLog.getProjectId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Stage log project does not match task project");
        }
        taskRepository.saveStageLog(stageLog);
    }

    private GenerateTask loadTask(Long projectId, Long taskId) {
        GenerateTask task = loadTask(taskId);
        if (!task.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Task does not belong to project");
        }
        return task;
    }

    private GenerateTask loadTask(Long taskId) {
        return taskRepository.findTaskById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Task does not exist"));
    }

    private TaskResponse toTaskResponse(GenerateTask task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setProjectId(task.getProjectId());
        response.setTaskType(task.getTaskType());
        response.setBizType(task.getBizType());
        response.setBizId(task.getBizId());
        response.setStatus(task.getStatus() == null ? null : task.getStatus().name());
        response.setCurrentStage(task.getCurrentStage() == null ? null : task.getCurrentStage().name());
        response.setRetryCount(task.getRetryCount());
        response.setMaxRetryCount(task.getMaxRetryCount());
        response.setErrorMessage(task.getErrorMessage());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }

    private TaskStageLogResponse toStageLogResponse(TaskStageLog stageLog) {
        TaskStageLogResponse response = new TaskStageLogResponse();
        response.setStageLogId(stageLog.getId());
        response.setTaskId(stageLog.getTaskId());
        response.setProjectId(stageLog.getProjectId());
        response.setStageCode(stageLog.getStageCode() == null ? null : stageLog.getStageCode().name());
        response.setStatus(stageLog.getStatus() == null ? null : stageLog.getStatus().name());
        response.setInputSummary(stageLog.getInputSummary());
        response.setOutputSummary(stageLog.getOutputSummary());
        response.setErrorMessage(stageLog.getErrorMessage());
        response.setStartedAt(stageLog.getStartedAt());
        response.setFinishedAt(stageLog.getFinishedAt());
        response.setCostMs(stageLog.getCostMs());
        response.setCreatedAt(stageLog.getCreatedAt());
        return response;
    }
}
