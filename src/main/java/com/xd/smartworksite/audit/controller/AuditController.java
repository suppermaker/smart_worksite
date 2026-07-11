package com.xd.smartworksite.audit.controller;

import com.xd.smartworksite.audit.application.AuditApplicationService;
import com.xd.smartworksite.audit.dto.AuditLogQueryRequest;
import com.xd.smartworksite.audit.dto.AuditLogResponse;
import com.xd.smartworksite.ai.application.AiApplicationService;
import com.xd.smartworksite.ai.dto.ExternalCallLogQueryRequest;
import com.xd.smartworksite.ai.dto.ExternalCallLogResponse;
import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Validated
public class AuditController {
    private final AuditApplicationService auditApplicationService;
    private final AiApplicationService aiApplicationService;

    public AuditController(AuditApplicationService auditApplicationService,
                           AiApplicationService aiApplicationService) {
        this.auditApplicationService = auditApplicationService;
        this.aiApplicationService = aiApplicationService;
    }

    @GetMapping("/logs")
    public ApiResponse<PageResult<AuditLogResponse>> queryAuditLogs(@Valid AuditLogQueryRequest request) {
        return ApiResponse.success(auditApplicationService.queryAuditLogs(request));
    }

    @GetMapping("/external-call-logs")
    public ApiResponse<PageResult<ExternalCallLogResponse>> queryExternalCallLogs(@Valid ExternalCallLogQueryRequest request) {
        return ApiResponse.success(aiApplicationService.queryExternalCallLogs(request));
    }
}
