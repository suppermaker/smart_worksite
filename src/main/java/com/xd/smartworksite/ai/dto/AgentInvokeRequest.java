package com.xd.smartworksite.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentInvokeRequest {
    @NotNull
    private Long projectId;
    @NotBlank
    private String goal;
    private List<String> tools = new ArrayList<>();
    @Valid
    private List<AiMessage> contextMessages = new ArrayList<>();
    private Map<String, Object> parameters = new LinkedHashMap<>();
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public List<String> getTools() { return tools; }
    public void setTools(List<String> tools) { this.tools = tools; }
    public List<AiMessage> getContextMessages() { return contextMessages; }
    public void setContextMessages(List<AiMessage> contextMessages) { this.contextMessages = contextMessages; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
