package com.xd.smartworksite.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class QaFeedbackRequest {
    @NotBlank
    @Size(max = 32)
    private String feedbackType;
    @Size(max = 1000)
    private String comment;
    private Map<String, Object> extra = new LinkedHashMap<>();

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
