package com.xd.smartworksite.template.dto;

import java.io.InputStream;

public class TemplatePreviewFile {

    private final String fileName;
    private final String contentType;
    private final long fileSize;
    private final InputStream inputStream;

    public TemplatePreviewFile(String fileName, String contentType, long fileSize, InputStream inputStream) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.inputStream = inputStream;
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
