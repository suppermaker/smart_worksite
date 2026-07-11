package com.xd.smartworksite.knowledge.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class KnowledgeDocumentUploadRequest {
    private MultipartFile file;

    @Size(max = 255)
    private String title;

    @Size(max = 64)
    private String sourceType;

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
