package com.xd.smartworksite.task.application;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskStageLog;
import com.xd.smartworksite.task.domain.TaskStatus;
import com.xd.smartworksite.task.domain.TaskStatusCount;
import com.xd.smartworksite.task.dto.TaskQueryRequest;
import com.xd.smartworksite.task.dto.TaskResponse;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import com.xd.smartworksite.task.dto.TaskStatisticsResponse;
import com.xd.smartworksite.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskApplicationService {
    private static final String STAGE_RETRY_REQUESTED = "RETRY_REQUESTED";
    private static final String STAGE_CANCEL_REQUESTED = "CANCEL_REQUESTED";

    private final TaskRepository taskRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final TaskOutboxApplicationService taskOutboxApplicationService;

    public TaskApplicationService(TaskRepository taskRepository,
                                  ProjectAccessApplicationService projectAccessApplicationService,
                                  TaskOutboxApplicationService taskOutboxApplicationService) {
        this.taskRepository = taskRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.taskOutboxApplicationService = taskOutboxApplicationService;
    }

    public TaskResponse getTask(Long taskId) {
        GenerateTask task = requireTask(taskId);
        projectAccessApplicationService.requireProjectAccess(task.getProjectId());
        return toResponse(task);
    }

    public List<TaskStageLogResponse> getStages(Long taskId) {
        GenerateTask task = requireTask(taskId);
        projectAccessApplicationService.requireProjectAccess(task.getProjectId());
        return taskRepository.findStages(taskId).stream().map(this::toStageResponse).toList();
    }

    public PageResult<TaskResponse> queryTasks(TaskQueryRequest request) {
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        }
        String status = normalizeStatus(request.getStatus());
        String taskType = trimToNull(request.getTaskType());
        List<Long> accessibleProjectIds = request.getProjectId() == null && !SecurityUtils.isPlatformAdmin()
                ? projectAccessApplicationService.currentUserAccessibleProjectIds()
                : null;
        if (request.getProjectId() == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return new PageResult<>(request.getPageNo(), request.getPageSize(), 0, List.of());
        }
        Page<GenerateTask> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> taskRepository.findPage(
                        request.getProjectId(),
                        accessibleProjectIds,
                        taskType,
                        status,
                        request.getCreatedFrom(),
                        request.getCreatedTo()
                ));
        List<TaskResponse> records = page.getResult().stream().map(this::toResponse).toList();
        return new PageResult<>(request.getPageNo(), request.getPageSize(), page.getTotal(), records);
    }

    public TaskStatisticsResponse statistics(Long projectId) {
        if (projectId != null) {
            projectAccessApplicationService.requireProjectAccess(projectId);
        }
        List<Long> accessibleProjectIds = projectId == null && !SecurityUtils.isPlatformAdmin()
                ? projectAccessApplicationService.currentUserAccessibleProjectIds()
                : null;
        if (projectId == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            TaskStatisticsResponse response = new TaskStatisticsResponse();
            response.setProjectId(null);
            response.setStatusCounts(Map.of());
            return response;
        }
        List<TaskStatusCount> counts = taskRepository.countByStatus(projectId, accessibleProjectIds);
        Map<String, Long> statusCounts = counts.stream()
                .collect(Collectors.toMap(TaskStatusCount::getStatus, TaskStatusCount::getCount, Long::sum));
        TaskStatisticsResponse response = new TaskStatisticsResponse();
        response.setProjectId(projectId);
        response.setStatusCounts(statusCounts);
        response.setQueuedCount(statusCounts.getOrDefault(TaskStatus.QUEUED.name(), 0L)
                + statusCounts.getOrDefault(TaskStatus.PENDING.name(), 0L)
                + statusCounts.getOrDefault(TaskStatus.RETRYING.name(), 0L));
        response.setRunningCount(statusCounts.getOrDefault(TaskStatus.RUNNING.name(), 0L));
        response.setFailedCount(statusCounts.getOrDefault(TaskStatus.FAILED.name(), 0L));
        return response;
    }

    @Transactional
    public TaskResponse retryTask(Long taskId) {
        GenerateTask task = requireTask(taskId);
        projectAccessApplicationService.requireProjectWritableAccess(task.getProjectId());
        TaskStatus status = TaskStatus.parse(task.getStatus());
        if (!status.canRetry()) {
            throw new BusinessException(ErrorCode.CONFLICT, "task is not retryable");
        }
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? 0 : task.getMaxRetryCount();
        if (retryCount >= maxRetryCount) {
            throw new BusinessException(ErrorCode.CONFLICT, "task retry limit exceeded");
        }
        int updated = taskRepository.markRetrying(taskId, TaskStatus.QUEUED.name(), STAGE_RETRY_REQUESTED, SecurityUtils.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "task retry state changed");
        }
        insertStage(task, STAGE_RETRY_REQUESTED, TaskStatus.QUEUED.name(), "retry requested", null);
        task.setStatus(TaskStatus.QUEUED.name());
        taskOutboxApplicationService.enqueueTask(task, "retry requested");
        return getTask(taskId);
    }

    @Transactional
    public TaskResponse cancelTask(Long taskId) {
        GenerateTask task = requireTask(taskId);
        projectAccessApplicationService.requireProjectWritableAccess(task.getProjectId());
        TaskStatus status = TaskStatus.parse(task.getStatus());
        if (!status.canCancel()) {
            throw new BusinessException(ErrorCode.CONFLICT, "task is not cancelable");
        }
        int updated;
        if (status == TaskStatus.RUNNING) {
            updated = taskRepository.requestRunningCancel(taskId, SecurityUtils.getCurrentUserId());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "task cancel state changed");
            }
            insertStage(task, STAGE_CANCEL_REQUESTED, TaskStatus.RUNNING.name(), "running task cancel requested", null);
        } else {
            updated = taskRepository.cancelWaiting(taskId, SecurityUtils.getCurrentUserId());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "task cancel state changed");
            }
            insertStage(task, STAGE_CANCEL_REQUESTED, TaskStatus.CANCELED.name(), "waiting task canceled", null);
        }
        return getTask(taskId);
    }

    private GenerateTask requireTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "taskId is required");
        }
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "task not found"));
    }

    private void insertStage(GenerateTask task, String stageCode, String status, String outputSummary, String errorMessage) {
        TaskStageLog log = new TaskStageLog();
        log.setProjectId(task.getProjectId());
        log.setTaskId(task.getId());
        log.setAttemptNo((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        log.setStageCode(stageCode);
        log.setStatus(status);
        log.setOutputSummary(outputSummary);
        log.setErrorMessage(errorMessage);
        log.setStartedAt(LocalDateTime.now());
        log.setFinishedAt(log.getStartedAt());
        log.setCostMs(0L);
        int inserted = taskRepository.insertStage(log);
        if (inserted == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "task stage log insert failed");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return TaskStatus.parse(status).name();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private TaskResponse toResponse(GenerateTask task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setProjectId(task.getProjectId());
        response.setTaskType(task.getTaskType());
        response.setBizType(task.getBizType());
        response.setBizId(task.getBizId());
        response.setStatus(task.getStatus());
        response.setCurrentStage(task.getCurrentStage());
        response.setRetryCount(task.getRetryCount());
        response.setMaxRetryCount(task.getMaxRetryCount());
        response.setCancelRequested(Boolean.TRUE.equals(task.getCancelRequested()));
        response.setErrorMessage(task.getErrorMessage());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    private TaskStageLogResponse toStageResponse(TaskStageLog log) {
        TaskStageLogResponse response = new TaskStageLogResponse();
        response.setId(log.getId());
        response.setTaskId(log.getTaskId());
        response.setAttemptNo(log.getAttemptNo());
        response.setStageCode(log.getStageCode());
        response.setStatus(log.getStatus());
        response.setInputSummary(log.getInputSummary());
        response.setOutputSummary(log.getOutputSummary());
        response.setErrorMessage(log.getErrorMessage());
        response.setStartedAt(log.getStartedAt());
        response.setFinishedAt(log.getFinishedAt());
        response.setCostMs(log.getCostMs());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
