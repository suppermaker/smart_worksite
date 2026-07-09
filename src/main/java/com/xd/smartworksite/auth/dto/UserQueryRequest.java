package com.xd.smartworksite.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class UserQueryRequest {

    private String keyword;
    private String status;

    @Min(1)
    private int pageNo = 1;

    @Min(1) @Max(100)
    private int pageSize = 20;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
