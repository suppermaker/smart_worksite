package com.xd.smartworksite.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class OcrTypeResponse {
    private String ocrType;
    private String name;
    private List<String> requiredFields = new ArrayList<>();

    public OcrTypeResponse() {}

    public OcrTypeResponse(String ocrType, String name, List<String> requiredFields) {
        this.ocrType = ocrType;
        this.name = name;
        this.requiredFields = requiredFields;
    }

    public String getOcrType() { return ocrType; }
    public void setOcrType(String ocrType) { this.ocrType = ocrType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getRequiredFields() { return requiredFields; }
    public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
}
