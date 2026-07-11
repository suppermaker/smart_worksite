package com.xd.smartworksite.ocr.domain;

import java.util.Locale;

public enum InvoiceType {
    VAT_SPECIAL,
    VAT_NORMAL;

    public static InvoiceType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("invoice type is required");
        }
        return InvoiceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
