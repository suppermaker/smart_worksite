package com.xd.smartworksite.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ExternalCallLogQueryRequest {
    private Long projectId;
    private String serviceName;
    private String callType;
    private String status;
    @Min(1)
    private Integer pageNo = 1;
    @Min(1) @Max(100)
    private Integer pageSize = 20;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
