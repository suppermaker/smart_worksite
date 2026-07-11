package com.xd.smartworksite.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.audit.domain.AuditLog;
import com.xd.smartworksite.audit.dto.AuditLogQueryRequest;
import com.xd.smartworksite.audit.dto.AuditLogResponse;
import com.xd.smartworksite.audit.repository.AuditLogRepository;
import com.xd.smartworksite.common.config.RequestContext;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@Service
public class AuditApplicationService {
    private final AuditLogRepository auditLogRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final ObjectMapper objectMapper;

    public AuditApplicationService(AuditLogRepository auditLogRepository,
                                   ProjectAccessApplicationService projectAccessApplicationService,
                                   ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.objectMapper = objectMapper;
    }

    public void record(Long projectId, String action, String objectType, Long objectId, Map<String, Object> detail) {
        AuditLog log = new AuditLog();
        log.setProjectId(projectId);
        log.setOperatorId(SecurityUtils.getCurrentUserId());
        log.setAction(requireText(action, "audit action is required"));
        log.setObjectType(requireText(objectType, "audit objectType is required"));
        log.setObjectId(objectId);
        log.setRequestId(MDC.get(RequestContext.REQUEST_ID_MDC_KEY));
        log.setIpAddress(currentIpAddress());
        log.setDetail(toJson(detail == null ? Map.of() : detail));
        int inserted = auditLogRepository.insert(log);
        if (inserted <= 0 || log.getId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "audit log insert failed");
        }
    }

    public PageResult<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request) {
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        }
        List<Long> accessibleProjectIds = request.getProjectId() == null && !SecurityUtils.isPlatformAdmin()
                ? projectAccessApplicationService.currentUserAccessibleProjectIds()
                : null;
        if (request.getProjectId() == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return new PageResult<>(request.getPageNo(), request.getPageSize(), 0, List.of());
        }
        Page<AuditLog> page = PageHelper.startPage(request.getPageNo(), request.getPageSize());
        List<AuditLog> records = auditLogRepository.findPage(
                request.getProjectId(),
                accessibleProjectIds,
                request.getOperatorId(),
                trimToNull(request.getAction()),
                trimToNull(request.getObjectType()),
                request.getCreatedFrom(),
                request.getCreatedTo()
        );
        return new PageResult<>(
                request.getPageNo(),
                request.getPageSize(),
                page.getTotal(),
                records.stream().map(this::toResponse).toList()
        );
    }

    private String currentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",", 2)[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "audit detail json serialization failed");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setProjectId(log.getProjectId());
        response.setOperatorId(log.getOperatorId());
        response.setAction(log.getAction());
        response.setObjectType(log.getObjectType());
        response.setObjectId(log.getObjectId());
        response.setRequestId(log.getRequestId());
        response.setIpAddress(log.getIpAddress());
        response.setDetail(log.getDetail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
