package com.xd.smartworksite.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ContextPrepareRequest {
    @NotNull
    private Long projectId;
    @Valid
    private List<AiMessage> messages = new ArrayList<>();
    @NotBlank
    private String currentQuestion;
    private Integer maxContextLength = 6000;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public List<AiMessage> getMessages() { return messages; }
    public void setMessages(List<AiMessage> messages) { this.messages = messages; }
    public String getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(String currentQuestion) { this.currentQuestion = currentQuestion; }
    public Integer getMaxContextLength() { return maxContextLength; }
    public void setMaxContextLength(Integer maxContextLength) { this.maxContextLength = maxContextLength; }
}
