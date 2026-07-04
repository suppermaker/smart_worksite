package com.xd.smartworksite.task.domain;

public class TaskQueueMessage {

    private Long taskId;
    private Long projectId;
    private String taskType;

    public TaskQueueMessage() {
    }

    public TaskQueueMessage(Long taskId, Long projectId, String taskType) {
        this.taskId = taskId;
        this.projectId = projectId;
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

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
