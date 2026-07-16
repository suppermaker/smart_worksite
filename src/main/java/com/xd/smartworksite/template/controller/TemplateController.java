package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.application.TemplatePreviewApplicationService;
import com.xd.smartworksite.template.application.TemplateVariableApplicationService;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplatePreviewFile;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateStatusResponse;
import com.xd.smartworksite.template.dto.TemplateUpdateRequest;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionResponse;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@Validated
public class TemplateController {

    private final TemplateApplicationService templateApplicationService;
    private final TemplatePreviewApplicationService templatePreviewApplicationService;
    private final TemplateVariableApplicationService templateVariableApplicationService;

    public TemplateController(TemplateApplicationService templateApplicationService,
                              TemplatePreviewApplicationService templatePreviewApplicationService,
                              TemplateVariableApplicationService templateVariableApplicationService) {
        this.templateApplicationService = templateApplicationService;
        this.templatePreviewApplicationService = templatePreviewApplicationService;
        this.templateVariableApplicationService = templateVariableApplicationService;
    }

    @PostMapping
    public ApiResponse<TemplateResponse> uploadTemplate(@RequestParam Long projectId,
                                                        @RequestParam String templateCategory,
                                                        @RequestParam String templateName,
                                                        @RequestParam String templateType,
                                                        @RequestParam(required = false) String scenario,
                                                        @RequestParam(required = false) String versionNo,
                                                        @RequestParam(required = false) String description,
                                                        @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, templateCategory, templateName, templateType, scenario, versionNo, description, file));
    }

    @GetMapping
    public ApiResponse<PageResult<TemplateResponse>> listTemplates(@Valid TemplateQueryRequest request) {
        return ApiResponse.success(templateApplicationService.queryTemplates(request));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(templateApplicationService.getTemplate(templateId));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateResponse> updateTemplate(@PathVariable Long templateId,
                                                        @Valid @RequestBody TemplateUpdateRequest request) {
        return ApiResponse.success(templateApplicationService.updateTemplate(templateId, request));
    }

    @PostMapping("/{templateId}/enable")
    public ApiResponse<TemplateStatusResponse> enableTemplate(@PathVariable Long templateId) {
        TemplateResponse template = templateApplicationService.enableTemplate(templateId);
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    @PostMapping("/{templateId}/disable")
    public ApiResponse<TemplateStatusResponse> disableTemplate(@PathVariable Long templateId) {
        TemplateResponse template = templateApplicationService.disableTemplate(templateId);
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long templateId) {
        templateApplicationService.deleteTemplate(templateId);
        return ApiResponse.success();
    }

    @GetMapping("/{templateId}/preview")
    public ResponseEntity<InputStreamResource> previewTemplate(@PathVariable Long templateId) {
        TemplatePreviewFile preview = templatePreviewApplicationService.openPreview(templateId);
        ContentDisposition disposition = ContentDisposition.builder("inline")
                .filename(preview.getFileName(), StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(resolveMediaType(preview.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (preview.getFileSize() >= 0) {
            builder.contentLength(preview.getFileSize());
        }
        return builder.body(new InputStreamResource(preview.getInputStream()));
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @GetMapping("/{templateId}/variables")
    public ApiResponse<List<String>> listTemplateVariables(@PathVariable Long templateId) {
        return ApiResponse.success(templateVariableApplicationService.listVariables(templateId));
    }

    @PutMapping("/{templateId}/variables/descriptions")
    public ApiResponse<List<TemplateVariableDescriptionResponse>> upsertVariableDescriptions(
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateVariableDescriptionUpsertRequest request) {
        return ApiResponse.success(templateVariableApplicationService.upsertDescriptions(templateId, request));
    }

    @GetMapping("/{templateId}/variables/descriptions")
    public ApiResponse<List<TemplateVariableDescriptionResponse>> listVariableDescriptions(
            @PathVariable Long templateId) {
        return ApiResponse.success(templateVariableApplicationService.listDescriptions(templateId));
    }

    @PostMapping("/report")
    public ApiResponse<TemplateResponse> uploadReportTemplate(@RequestParam Long projectId,
                                                              @RequestParam String templateName,
                                                              @RequestParam String templateType,
                                                              @RequestParam(required = false) String scenario,
                                                              @RequestParam(required = false) String versionNo,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, TemplateCategory.REPORT.name(), templateName, templateType, scenario, versionNo, description, file));
    }

    @PostMapping("/review")
    public ApiResponse<TemplateResponse> uploadReviewTemplate(@RequestParam Long projectId,
                                                              @RequestParam String templateName,
                                                              @RequestParam String templateType,
                                                              @RequestParam(required = false) String scenario,
                                                              @RequestParam(required = false) String versionNo,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, TemplateCategory.REVIEW.name(), templateName, templateType, scenario, versionNo, description, file));
    }
}
