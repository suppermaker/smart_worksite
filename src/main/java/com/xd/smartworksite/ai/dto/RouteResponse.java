package com.xd.smartworksite.ai.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteResponse {
    private String routeType;
    private String reason;
    private List<Map<String, Object>> requiredResources = new ArrayList<>();
    private List<String> followUpQuestions = new ArrayList<>();
    private String providerTraceId;
    public String getRouteType() { return routeType; }
    public void setRouteType(String routeType) { this.routeType = routeType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<Map<String, Object>> getRequiredResources() { return requiredResources; }
    public void setRequiredResources(List<Map<String, Object>> requiredResources) { this.requiredResources = requiredResources; }
    public List<String> getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
}
