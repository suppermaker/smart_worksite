package com.xd.smartworksite.knowledge.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.knowledge.retrieval.http")
public class KnowledgeRetrievalProperties {

    private boolean enabled;
    private String endpoint;
    private String apiKey;
    private Integer timeoutMs;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
