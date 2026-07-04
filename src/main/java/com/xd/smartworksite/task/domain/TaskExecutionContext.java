package com.xd.smartworksite.task.domain;

public class TaskExecutionContext {

    private final Long taskId;
    private final Long projectId;
    private final Long userId;
    private final String requestId;
    private final String taskType;
    private final String bizType;
    private final Long bizId;

    public TaskExecutionContext(GenerateTask task) {
        this(task, null);
    }

    public TaskExecutionContext(GenerateTask task, TaskQueueMessage message) {
        this.taskId = task.getId();
        this.projectId = task.getProjectId();
        this.userId = message == null ? null : message.getUserId();
        this.requestId = message == null ? null : message.getRequestId();
        this.taskType = task.getTaskType();
        this.bizType = task.getBizType();
        this.bizId = task.getBizId();
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getBizType() {
        return bizType;
    }

    public Long getBizId() {
        return bizId;
    }
}
