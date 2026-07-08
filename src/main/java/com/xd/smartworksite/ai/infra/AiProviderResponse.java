package com.xd.smartworksite.ai.infra;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiProviderResponse {
    private boolean success;
    private String traceId;
    private Map<String, Object> data = new LinkedHashMap<>();
    private Map<String, Object> usage = new LinkedHashMap<>();
    private String errorCode;
    private String errorMessage;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public Map<String, Object> getUsage() { return usage; }
    public void setUsage(Map<String, Object> usage) { this.usage = usage; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
