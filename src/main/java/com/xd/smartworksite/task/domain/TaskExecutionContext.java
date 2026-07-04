package com.xd.smartworksite.task.domain;

public class TaskExecutionContext {

    private final Long taskId;
    private final Long projectId;
    private final String taskType;
    private final String bizType;
    private final Long bizId;

    public TaskExecutionContext(GenerateTask task) {
        this.taskId = task.getId();
        this.projectId = task.getProjectId();
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
