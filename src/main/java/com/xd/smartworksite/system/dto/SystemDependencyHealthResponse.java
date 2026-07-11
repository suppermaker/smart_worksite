package com.xd.smartworksite.system.dto;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SystemDependencyHealthResponse {
    private String status;
    private OffsetDateTime checkedAt;
    private Map<String, DependencyStatus> dependencies = new LinkedHashMap<>();

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(OffsetDateTime checkedAt) { this.checkedAt = checkedAt; }
    public Map<String, DependencyStatus> getDependencies() { return dependencies; }
    public void setDependencies(Map<String, DependencyStatus> dependencies) { this.dependencies = dependencies; }

    public static class DependencyStatus {
        private String status;
        private Long elapsedMs;
        private String errorMessage;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getElapsedMs() { return elapsedMs; }
        public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
