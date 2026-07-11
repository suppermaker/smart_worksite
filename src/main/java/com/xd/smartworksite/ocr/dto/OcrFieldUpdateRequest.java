package com.xd.smartworksite.ocr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class OcrFieldUpdateRequest {
    @Valid
    @NotEmpty
    private List<OcrFieldResponse> fields = new ArrayList<>();

    public List<OcrFieldResponse> getFields() { return fields; }
    public void setFields(List<OcrFieldResponse> fields) { this.fields = fields; }
}
