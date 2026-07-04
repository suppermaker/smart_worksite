package com.xd.smartworksite.intelligence.dto;

import com.xd.smartworksite.intelligence.domain.RouteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelCallRequest {

    @NotNull
    private Long projectId;

    private Long userId;

    @Size(max = 128)
    private String requestId;

    private Long taskId;

    @NotNull
    private RouteMode routeMode = RouteMode.MODEL;

    @Size(max = 128)
    private String modelName;

    @Valid
    @NotEmpty
    @Size(max = 50)
    private List<ModelMessageRequest> messages = List.of();

    @Size(max = 20)
    private Map<String, Object> parameters = Map.of();

    @Min(100)
    @Max(60000)
    private Integer timeoutMs = 10000;

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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public RouteMode getRouteMode() {
        return routeMode;
    }

    public void setRouteMode(RouteMode routeMode) {
        this.routeMode = routeMode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public List<ModelMessageRequest> getMessages() {
        return messages;
    }

    public void setMessages(List<ModelMessageRequest> messages) {
        this.messages = messages == null ? List.of() : new ArrayList<>(messages);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? Map.of() : new LinkedHashMap<>(parameters);
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
