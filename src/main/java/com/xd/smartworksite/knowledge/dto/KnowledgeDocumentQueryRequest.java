package com.xd.smartworksite.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class KnowledgeDocumentQueryRequest {
    private String indexStatus;
    private String keyword;

    @Min(1)
    private int pageNo = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 20;

    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
