package com.xd.smartworksite.task.domain;

import java.time.LocalDateTime;

public class TaskOutboxEvent {
    private Long id;
    private Long taskId;
    private Long projectId;
    private String eventType;
    private String payload;
    private String status;
    private Integer deliveryCount;
    private LocalDateTime nextDeliveryAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDeliveryCount() { return deliveryCount; }
    public void setDeliveryCount(Integer deliveryCount) { this.deliveryCount = deliveryCount; }
    public LocalDateTime getNextDeliveryAt() { return nextDeliveryAt; }
    public void setNextDeliveryAt(LocalDateTime nextDeliveryAt) { this.nextDeliveryAt = nextDeliveryAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
