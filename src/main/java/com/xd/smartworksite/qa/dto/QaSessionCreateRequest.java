package com.xd.smartworksite.qa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class QaSessionCreateRequest {
    @NotNull
    private Long projectId;
    @Size(max = 255)
    private String title;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
