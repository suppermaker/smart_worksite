package com.xd.smartworksite.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewIssueUpdateRequest {
    @NotBlank
    @Size(max = 32)
    private String status;
    @Size(max = 1000)
    private String comment;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
