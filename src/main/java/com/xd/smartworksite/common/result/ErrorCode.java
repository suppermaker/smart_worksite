package com.xd.smartworksite.common.result;

public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未认证"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统错误"),
    EXTERNAL_SERVICE_ERROR(50200, "外部服务异常"),
    SERVICE_UNAVAILABLE(50300, "服务不可用");

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
