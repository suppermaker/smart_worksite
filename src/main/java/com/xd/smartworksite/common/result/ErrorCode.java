package com.xd.smartworksite.common.result;

public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "\u53c2\u6570\u9519\u8bef"),
    UNAUTHORIZED(40100, "\u672a\u767b\u5f55"),
    FORBIDDEN(40300, "\u65e0\u6743\u9650"),
    NOT_FOUND(40400, "\u8d44\u6e90\u4e0d\u5b58\u5728"),
    CONFLICT(40900, "\u8d44\u6e90\u51b2\u7a81"),
    TOO_MANY_REQUESTS(42900, "\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41"),
    SYSTEM_ERROR(50000, "\u7cfb\u7edf\u9519\u8bef"),
    EXTERNAL_SERVICE_ERROR(50200, "\u5916\u90e8\u670d\u52a1\u5f02\u5e38"),
    SERVICE_UNAVAILABLE(50300, "\u670d\u52a1\u4e0d\u53ef\u7528");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
