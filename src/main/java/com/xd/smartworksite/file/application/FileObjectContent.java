package com.xd.smartworksite.file.application;

import java.io.InputStream;

public class FileObjectContent {

    private final Long fileId;
    private final Long projectId;
    private final Long bizId;
    private final String fileName;
    private final String contentType;
    private final long fileSize;
    private final InputStream inputStream;

    public FileObjectContent(Long fileId,
                             Long projectId,
                             Long bizId,
                             String fileName,
                             String contentType,
                             long fileSize,
                             InputStream inputStream) {
        this.fileId = fileId;
        this.projectId = projectId;
        this.bizId = bizId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.inputStream = inputStream;
    }

    public Long getFileId() {
        return fileId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getBizId() {
        return bizId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
