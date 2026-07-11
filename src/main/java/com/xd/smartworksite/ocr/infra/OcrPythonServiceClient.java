package com.xd.smartworksite.ocr.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.ai.domain.ExternalCallLog;
import com.xd.smartworksite.ai.infra.AiProviderResponse;
import com.xd.smartworksite.ai.infra.AiPythonServiceProperties;
import com.xd.smartworksite.ai.repository.AiRepository;
import com.xd.smartworksite.common.config.RequestContext;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OcrPythonServiceClient {
    private static final Logger log = LoggerFactory.getLogger(OcrPythonServiceClient.class);
    private static final String SERVICE_NAME = "PYTHON_AI_SERVICE";
    private static final String CALL_TYPE = "OCR_RECOGNIZE";

    private final AiPythonServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final AiRepository aiRepository;
    private final HttpClient httpClient;

    public OcrPythonServiceClient(AiPythonServiceProperties properties,
                                  ObjectMapper objectMapper,
                                  AiRepository aiRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.aiRepository = aiRepository;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    public AiProviderResponse recognize(Long projectId, OcrProviderRequest payload) {
        long started = System.currentTimeMillis();
        String requestSummary = summarize(payload);
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl().replaceAll("/+$", "")
                            + properties.getPaths().getOcrRecognize()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                builder.header("X-AI-Service-Key", properties.getApiKey());
            }

            Exception lastError = null;
            for (int attempt = 0; attempt <= Math.max(0, properties.getRetryCount()); attempt++) {
                try {
                    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                                "Python OCR服务调用失败: HTTP " + response.statusCode());
                    }
                    AiProviderResponse providerResponse = objectMapper.readValue(response.body(), AiProviderResponse.class);
                    if (!providerResponse.isSuccess()) {
                        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                                providerResponse.getErrorMessage() == null ? "Python OCR服务返回失败" : providerResponse.getErrorMessage());
                    }
                    saveLog(projectId, requestSummary, summarize(providerResponse.getData()), "SUCCESS",
                            System.currentTimeMillis() - started, null);
                    return providerResponse;
                } catch (BusinessException ex) {
                    throw ex;
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Python OCR服务不可用: " + (lastError == null ? "unknown" : lastError.getMessage()));
        } catch (BusinessException ex) {
            saveLog(projectId, requestSummary, null, "FAILED", System.currentTimeMillis() - started, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            saveLog(projectId, requestSummary, null, "FAILED", System.currentTimeMillis() - started, ex.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Python OCR服务调用异常: " + ex.getMessage());
        }
    }

    private void saveLog(Long projectId, String requestSummary, String responseSummary,
                         String status, long costMs, String errorMessage) {
        try {
            ExternalCallLog callLog = new ExternalCallLog();
            callLog.setProjectId(projectId);
            callLog.setServiceName(SERVICE_NAME);
            callLog.setCallType(CALL_TYPE);
            callLog.setRequestId(MDC.get(RequestContext.REQUEST_ID_MDC_KEY));
            callLog.setRequestSummary(limit(requestSummary, 2000));
            callLog.setResponseSummary(limit(responseSummary, 2000));
            callLog.setStatus(status);
            callLog.setCostMs(costMs);
            callLog.setErrorMessage(limit(errorMessage, 2000));
            aiRepository.saveExternalCallLog(callLog);
        } catch (Exception ex) {
            log.error("ocr external call log persistence failed, projectId={}, status={}", projectId, status, ex);
        }
    }

    private String summarize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Object sanitized = sanitize(objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {}));
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception ex) {
            return limit(String.valueOf(value), 2000);
        }
    }

    @SuppressWarnings("unchecked")
    private Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lowerKey = key.toLowerCase();
                if (isSensitiveKey(lowerKey)) {
                    result.put(key, "***");
                } else {
                    result.put(key, sanitize(entry.getValue()));
                }
            }
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::sanitize).toList();
        }
        if (value instanceof String text) {
            return maskSensitiveText(limit(text, 200));
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        return key.contains("key")
                || key.contains("password")
                || key.contains("token")
                || key.contains("downloadurl")
                || key.contains("url")
                || key.contains("idnumber")
                || key.contains("address")
                || key.contains("base64")
                || key.contains("image");
    }

    private String maskSensitiveText(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\b\\d{17}[0-9Xx]\\b", "******************")
                .replaceAll("\\b1[3-9]\\d{9}\\b", "***********");
    }

    private String limit(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }
}
