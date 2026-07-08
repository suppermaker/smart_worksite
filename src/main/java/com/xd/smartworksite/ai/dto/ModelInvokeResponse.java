package com.xd.smartworksite.ai.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelInvokeResponse {
    private String answer;
    private String providerTraceId;
    private Map<String, Object> usage = new LinkedHashMap<>();
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
    public Map<String, Object> getUsage() { return usage; }
    public void setUsage(Map<String, Object> usage) { this.usage = usage; }
}
