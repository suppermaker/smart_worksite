package com.xd.smartworksite.auth.dto;

public class UserProjectResponse {

    private Long projectId;
    private String projectName;
    private String projectCode;
    private String status;
    private String projectRole;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProjectRole() { return projectRole; }
    public void setProjectRole(String projectRole) { this.projectRole = projectRole; }
}
