package com.xd.smartworksite.task.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskStatisticsResponse {
    private Long projectId;
    private Map<String, Long> statusCounts = new LinkedHashMap<>();
    private long queuedCount;
    private long runningCount;
    private long failedCount;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
    public long getQueuedCount() { return queuedCount; }
    public void setQueuedCount(long queuedCount) { this.queuedCount = queuedCount; }
    public long getRunningCount() { return runningCount; }
    public void setRunningCount(long runningCount) { this.runningCount = runningCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
}
