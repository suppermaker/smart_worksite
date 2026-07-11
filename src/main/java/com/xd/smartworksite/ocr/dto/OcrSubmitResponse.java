package com.xd.smartworksite.ocr.dto;

public class OcrSubmitResponse {
    private Long recordId;
    private Long taskId;
    private String status;

    public OcrSubmitResponse() {}

    public OcrSubmitResponse(Long recordId, Long taskId, String status) {
        this.recordId = recordId;
        this.taskId = taskId;
        this.status = status;
    }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
