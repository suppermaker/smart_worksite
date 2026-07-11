package com.xd.smartworksite.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class KnowledgeBaseQueryRequest {
    private String status;
    private String domain;
    private String keyword;

    @Min(1)
    private int pageNo = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 20;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
