package com.xd.smartworksite.qa.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.qa.application.QaApplicationService;
import com.xd.smartworksite.qa.dto.QaFeedbackRequest;
import com.xd.smartworksite.qa.dto.QaMessageDetailResponse;
import com.xd.smartworksite.qa.dto.QaMessageResponse;
import com.xd.smartworksite.qa.dto.QaMessageSendRequest;
import com.xd.smartworksite.qa.dto.QaSessionCreateRequest;
import com.xd.smartworksite.qa.dto.QaSessionQueryRequest;
import com.xd.smartworksite.qa.dto.QaSessionResponse;
import com.xd.smartworksite.qa.dto.QaSessionUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@Validated
public class QaController {
    private final QaApplicationService qaApplicationService;

    public QaController(QaApplicationService qaApplicationService) {
        this.qaApplicationService = qaApplicationService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<QaSessionResponse> createSession(@Valid @RequestBody QaSessionCreateRequest request) {
        return ApiResponse.success(qaApplicationService.createSession(request));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('qa:view')")
    public ApiResponse<PageResult<QaSessionResponse>> listSessions(@Valid QaSessionQueryRequest request) {
        return ApiResponse.success(qaApplicationService.querySessions(request));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('qa:view')")
    public ApiResponse<QaSessionResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success(qaApplicationService.getSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<QaSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody QaSessionUpdateRequest request) {
        return ApiResponse.success(qaApplicationService.updateSession(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<Void> archiveSession(@PathVariable Long sessionId) {
        qaApplicationService.archiveSession(sessionId);
        return ApiResponse.success();
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<QaMessageResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody QaMessageSendRequest request) {
        return ApiResponse.success(qaApplicationService.sendMessage(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAuthority('qa:view')")
    public ApiResponse<List<QaMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.success(qaApplicationService.getSessionMessages(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages/{messageId}/regenerate")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<QaMessageResponse> regenerate(
            @PathVariable Long sessionId,
            @PathVariable Long messageId) {
        return ApiResponse.success(qaApplicationService.regenerate(sessionId, messageId));
    }

    @GetMapping("/messages/{messageId}")
    @PreAuthorize("hasAuthority('qa:view')")
    public ApiResponse<QaMessageDetailResponse> getMessage(@PathVariable Long messageId) {
        return ApiResponse.success(qaApplicationService.getMessage(messageId));
    }

    @GetMapping("/messages/{messageId}/references")
    @PreAuthorize("hasAuthority('qa:view')")
    public ApiResponse<List<Map<String, Object>>> getReferences(@PathVariable Long messageId) {
        return ApiResponse.success(qaApplicationService.getMessageReferences(messageId));
    }

    @PostMapping("/messages/{messageId}/feedback")
    @PreAuthorize("hasAuthority('qa:manage')")
    public ApiResponse<QaMessageResponse> feedback(
            @PathVariable Long messageId,
            @Valid @RequestBody QaFeedbackRequest request) {
        return ApiResponse.success(qaApplicationService.feedback(messageId, request));
    }
}
