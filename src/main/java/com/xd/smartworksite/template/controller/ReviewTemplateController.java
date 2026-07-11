package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateStatusResponse;
import com.xd.smartworksite.template.dto.TemplateUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/review/templates")
@Validated
public class ReviewTemplateController {

    private final TemplateApplicationService templateApplicationService;

    public ReviewTemplateController(TemplateApplicationService templateApplicationService) {
        this.templateApplicationService = templateApplicationService;
    }

    @PostMapping
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

    @GetMapping
    public ApiResponse<List<TemplateResponse>> listReviewTemplates(TemplateQueryRequest request) {
        request.setTemplateCategory(TemplateCategory.REVIEW.name());
        return ApiResponse.success(templateApplicationService.listTemplates(request));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> getReviewTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(requireReviewTemplate(templateApplicationService.getTemplate(templateId)));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateResponse> updateReviewTemplate(@PathVariable Long templateId,
                                                              @Valid @RequestBody TemplateUpdateRequest request) {
        requireReviewTemplate(templateApplicationService.getTemplate(templateId));
        return ApiResponse.success(requireReviewTemplate(templateApplicationService.updateTemplate(templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteReviewTemplate(@PathVariable Long templateId) {
        requireReviewTemplate(templateApplicationService.getTemplate(templateId));
        templateApplicationService.deleteTemplate(templateId);
        return ApiResponse.success();
    }

    @PostMapping("/{templateId}/enable")
    public ApiResponse<TemplateStatusResponse> enableReviewTemplate(@PathVariable Long templateId) {
        requireReviewTemplate(templateApplicationService.getTemplate(templateId));
        TemplateResponse template = requireReviewTemplate(templateApplicationService.enableTemplate(templateId));
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    @PostMapping("/{templateId}/disable")
    public ApiResponse<TemplateStatusResponse> disableReviewTemplate(@PathVariable Long templateId) {
        requireReviewTemplate(templateApplicationService.getTemplate(templateId));
        TemplateResponse template = requireReviewTemplate(templateApplicationService.disableTemplate(templateId));
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    private TemplateResponse requireReviewTemplate(TemplateResponse template) {
        if (!TemplateCategory.REVIEW.name().equals(template.getTemplateCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "template is not a review template");
        }
        return template;
    }
}
