package com.xd.smartworksite.task.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.task.worker")
public class TaskWorkerProperties {
    private boolean enabled;
    private String workerId = "smart-worksite-worker";
    private long pollDelayMs = 2000;
    private long popTimeoutMs = 1000;
    private long leaseSeconds = 300;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public long getPollDelayMs() { return pollDelayMs; }
    public void setPollDelayMs(long pollDelayMs) { this.pollDelayMs = pollDelayMs; }
    public long getPopTimeoutMs() { return popTimeoutMs; }
    public void setPopTimeoutMs(long popTimeoutMs) { this.popTimeoutMs = popTimeoutMs; }
    public long getLeaseSeconds() { return leaseSeconds; }
    public void setLeaseSeconds(long leaseSeconds) { this.leaseSeconds = leaseSeconds; }
}
