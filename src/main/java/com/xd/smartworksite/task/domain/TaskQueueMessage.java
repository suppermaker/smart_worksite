package com.xd.smartworksite.task.domain;

public class TaskQueueMessage {

    private Long taskId;
    private Long projectId;
    private Long userId;
    private String requestId;
    private String taskType;

    public TaskQueueMessage() {
    }

    public TaskQueueMessage(Long taskId, Long projectId, String taskType) {
        this(taskId, projectId, null, null, taskType);
    }

    public TaskQueueMessage(Long taskId, Long projectId, Long userId, String requestId, String taskType) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.userId = userId;
        this.requestId = requestId;
        this.taskType = taskType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
