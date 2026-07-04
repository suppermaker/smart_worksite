package com.xd.smartworksite.intelligence.dto;

import com.xd.smartworksite.intelligence.domain.ExternalCallSummary;
import com.xd.smartworksite.intelligence.domain.ModelCallStatus;

public class ModelCallResponse {

    private String provider;
    private String modelName;
    private String content;
    private String structuredJson;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long costMs;
    private ModelCallStatus status;
    private String errorCode;
    private String errorMessage;
    private ExternalCallSummary externalCallSummary;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStructuredJson() {
        return structuredJson;
    }

    public void setStructuredJson(String structuredJson) {
        this.structuredJson = structuredJson;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public ModelCallStatus getStatus() {
        return status;
    }

    public void setStatus(ModelCallStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ExternalCallSummary getExternalCallSummary() {
        return externalCallSummary;
    }

    public void setExternalCallSummary(ExternalCallSummary externalCallSummary) {
        this.externalCallSummary = externalCallSummary;
    }
}
