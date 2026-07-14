package com.xd.smartworksite.ocr.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.ai.infra.AiProviderResponse;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.file.application.FileObjectApplicationService;
import com.xd.smartworksite.file.dto.FileAccessUrlResponse;
import com.xd.smartworksite.file.dto.FileObjectResponse;
import com.xd.smartworksite.ocr.domain.OcrRecord;
import com.xd.smartworksite.ocr.domain.OcrStatus;
import com.xd.smartworksite.ocr.domain.TaskStageLog;
import com.xd.smartworksite.ocr.infra.OcrProviderRequest;
import com.xd.smartworksite.ocr.infra.OcrPythonServiceClient;
import com.xd.smartworksite.ocr.repository.OcrRepository;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OcrRecognitionWorker {
    private static final Logger log = LoggerFactory.getLogger(OcrRecognitionWorker.class);
    private static final String TASK_STATUS_RUNNING = "RUNNING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STATUS_FAILED = "FAILED";
    private static final String STAGE_OCR_RECOGNITION = "OCR_RECOGNITION";
    private static final int FILE_URL_EXPIRE_SECONDS = 600;

    private final OcrRepository ocrRepository;
    private final FileObjectApplicationService fileObjectApplicationService;
    private final OcrPythonServiceClient ocrPythonServiceClient;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final ObjectMapper objectMapper;

    public OcrRecognitionWorker(OcrRepository ocrRepository,
                                FileObjectApplicationService fileObjectApplicationService,
                                OcrPythonServiceClient ocrPythonServiceClient,
                                ProjectAccessApplicationService projectAccessApplicationService,
                                ObjectMapper objectMapper) {
        this.ocrRepository = ocrRepository;
        this.fileObjectApplicationService = fileObjectApplicationService;
        this.ocrPythonServiceClient = ocrPythonServiceClient;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.objectMapper = objectMapper;
    }

    @Async("ocrTaskExecutor")
    public void recognizeAsync(Long recordId) {
        recognize(recordId);
    }

    public void recognize(Long recordId) {
        long started = System.currentTimeMillis();
        OcrRecord record = ocrRepository.findRecordById(recordId).orElseThrow();
        try {
            ocrRepository.updateRecordStatus(recordId, OcrStatus.PROCESSING.name(), null);
            ocrRepository.updateTaskStatus(record.getTaskId(), TASK_STATUS_RUNNING, STAGE_OCR_RECOGNITION, null);
            saveStageLog(record, TASK_STATUS_RUNNING, "OCR识别开始", null, null, null);

            projectAccessApplicationService.requireProjectWritableForSystem(record.getProjectId());
            FileObjectResponse file = fileObjectApplicationService.getFileForSystem(record.getFileId());
            if (!record.getProjectId().equals(file.getProjectId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "OCR file project mismatch");
            }
            FileAccessUrlResponse accessUrl = fileObjectApplicationService.createAccessUrlForSystem(
                    record.getFileId(), "DOWNLOAD", FILE_URL_EXPIRE_SECONDS);
            OcrProviderRequest request = buildProviderRequest(record, file, accessUrl.getUrl());
            AiProviderResponse providerResponse = ocrPythonServiceClient.recognize(record.getProjectId(), request);

            String fieldsJson = buildFieldsJson(record, providerResponse, System.currentTimeMillis() - started);
            ocrRepository.updateRecordSuccess(recordId, fieldsJson);
            ocrRepository.updateTaskStatus(record.getTaskId(), TASK_STATUS_SUCCESS, "FINISH", null);
            saveStageLog(record, TASK_STATUS_SUCCESS, null, "OCR识别完成", null, System.currentTimeMillis() - started);
        } catch (Exception ex) {
            String message = normalizeErrorMessage(ex);
            log.warn("ocr recognition failed, recordId={}", recordId, ex);
            ocrRepository.updateRecordStatus(recordId, OcrStatus.FAILED.name(), message);
            ocrRepository.updateTaskStatus(record.getTaskId(), TASK_STATUS_FAILED, STAGE_OCR_RECOGNITION, message);
            saveStageLog(record, TASK_STATUS_FAILED, null, null, message, System.currentTimeMillis() - started);
        }
    }

    private OcrProviderRequest buildProviderRequest(OcrRecord record, FileObjectResponse file, String downloadUrl) {
        OcrProviderRequest request = new OcrProviderRequest();
        request.setProjectId(record.getProjectId());
        request.setRecordId(record.getId());
        request.setOcrType(record.getOcrType());

        OcrProviderRequest.FilePayload filePayload = new OcrProviderRequest.FilePayload();
        filePayload.setFileId(file.getFileId());
        filePayload.setFileName(file.getFileName());
        filePayload.setContentType(file.getContentType());
        filePayload.setDownloadUrl(downloadUrl);
        request.setFile(filePayload);

        Map<String, Object> options = new LinkedHashMap<>();
        if (record.getCustomFieldsJson() != null && !record.getCustomFieldsJson().isBlank()) {
            options.putAll(parseJsonObject(record.getCustomFieldsJson()));
        }
        request.setOptions(options);
        return request;
    }

    private String buildFieldsJson(OcrRecord record, AiProviderResponse providerResponse, long elapsedMs) {
        Map<String, Object> data = providerResponse.getData() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(providerResponse.getData());
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ocrType", data.getOrDefault("ocrType", record.getOcrType()));
        summary.put("confidence", data.getOrDefault("confidence", 0));
        summary.put("provider", "QWEN_VL");
        summary.put("providerTraceId", providerResponse.getTraceId());
        summary.put("elapsedMs", elapsedMs);
        Object model = providerResponse.getUsage() == null ? null : providerResponse.getUsage().get("model");
        if (model != null) {
            summary.put("model", model);
        }
        result.put("summary", summary);
        result.put("fields", normalizeFields(data.get("fields")));
        result.put("extras", data.getOrDefault("extras", Map.of()));
        result.put("raw", data.getOrDefault("raw", Map.of()));
        return writeJson(result);
    }

    private List<Object> normalizeFields(Object fields) {
        if (fields instanceof List<?> list) {
            return list.stream().map(item -> (Object) item).toList();
        }
        return List.of();
    }

    private Map<String, Object> parseJsonObject(String json) {
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((key, item) -> result.put(String.valueOf(key), item));
                return result;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("customFields", value);
            return result;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(com.xd.smartworksite.common.result.ErrorCode.SYSTEM_ERROR, "OCR结果序列化失败");
        }
    }

    private void saveStageLog(OcrRecord record, String status, String input, String output, String error, Long costMs) {
        TaskStageLog stageLog = new TaskStageLog();
        stageLog.setProjectId(record.getProjectId());
        stageLog.setTaskId(record.getTaskId());
        stageLog.setStageCode(STAGE_OCR_RECOGNITION);
        stageLog.setStatus(status);
        stageLog.setInputSummary(input);
        stageLog.setOutputSummary(output);
        stageLog.setErrorMessage(error);
        stageLog.setCostMs(costMs);
        ocrRepository.saveStageLog(stageLog);
    }

    private String normalizeErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
