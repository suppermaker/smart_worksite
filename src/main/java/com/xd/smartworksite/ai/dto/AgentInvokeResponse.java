package com.xd.smartworksite.ai.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentInvokeResponse {
    private String result;
    private List<Map<String, Object>> steps = new ArrayList<>();
    private List<String> followUpQuestions = new ArrayList<>();
    private String providerTraceId;
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public List<Map<String, Object>> getSteps() { return steps; }
    public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }
    public List<String> getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
}
