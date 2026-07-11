package com.xd.smartworksite.ocr.domain;

import java.util.Locale;

public enum OcrType {
    ID_CARD,
    LICENSE_PLATE,
    INVOICE,
    CUSTOM;

    public static OcrType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ocr type is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("CONTRACT".equals(normalized)) {
            return CUSTOM;
        }
        return OcrType.valueOf(normalized);
    }
}
