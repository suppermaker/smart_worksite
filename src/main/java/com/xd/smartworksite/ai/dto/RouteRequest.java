package com.xd.smartworksite.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class RouteRequest {
    @NotNull
    private Long projectId;
    @NotBlank
    private String question;
    private List<Long> availableKnowledgeBaseIds = new ArrayList<>();
    private List<Long> availableDataSourceIds = new ArrayList<>();
    @Valid
    private List<AiMessage> contextMessages = new ArrayList<>();
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<Long> getAvailableKnowledgeBaseIds() { return availableKnowledgeBaseIds; }
    public void setAvailableKnowledgeBaseIds(List<Long> availableKnowledgeBaseIds) { this.availableKnowledgeBaseIds = availableKnowledgeBaseIds; }
    public List<Long> getAvailableDataSourceIds() { return availableDataSourceIds; }
    public void setAvailableDataSourceIds(List<Long> availableDataSourceIds) { this.availableDataSourceIds = availableDataSourceIds; }
    public List<AiMessage> getContextMessages() { return contextMessages; }
    public void setContextMessages(List<AiMessage> contextMessages) { this.contextMessages = contextMessages; }
}
