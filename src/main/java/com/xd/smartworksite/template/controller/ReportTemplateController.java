package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.application.TemplateVariableApplicationService;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/report/templates")
@Validated
public class ReportTemplateController {

    private final TemplateApplicationService templateApplicationService;
    private final TemplateVariableApplicationService templateVariableApplicationService;

    public ReportTemplateController(TemplateApplicationService templateApplicationService,
                                    TemplateVariableApplicationService templateVariableApplicationService) {
        this.templateApplicationService = templateApplicationService;
        this.templateVariableApplicationService = templateVariableApplicationService;
    }

    @PostMapping
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

    @GetMapping
    public ApiResponse<List<TemplateResponse>> listReportTemplates(TemplateQueryRequest request) {
        request.setTemplateCategory(TemplateCategory.REPORT.name());
        return ApiResponse.success(templateApplicationService.listTemplates(request));
    }

    @GetMapping("/{templateId}/variables")
    public ApiResponse<List<String>> listReportTemplateVariables(@PathVariable Long templateId) {
        return ApiResponse.success(templateVariableApplicationService.listReportVariables(templateId));
    }
}
