package com.xd.smartworksite.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DatabaseQueryRequest {
    @NotNull
    private Long projectId;
    @NotNull
    private Long dataSourceId;
    @NotBlank
    private String question;
    private String context;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
