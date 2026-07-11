package com.xd.smartworksite.knowledge.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.knowledge.application.KnowledgeBaseApplicationService;
import com.xd.smartworksite.knowledge.dto.KnowledgeBaseCreateRequest;
import com.xd.smartworksite.knowledge.dto.KnowledgeBaseQueryRequest;
import com.xd.smartworksite.knowledge.dto.KnowledgeBaseResponse;
import com.xd.smartworksite.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.xd.smartworksite.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.xd.smartworksite.knowledge.dto.KnowledgeDocumentResponse;
import com.xd.smartworksite.knowledge.dto.KnowledgeDocumentUploadRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class KnowledgeBaseController {
    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;

    public KnowledgeBaseController(KnowledgeBaseApplicationService knowledgeBaseApplicationService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
    }

    @PostMapping("/api/projects/{projectId}/knowledge-bases")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(
            @PathVariable Long projectId,
            @Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.success(knowledgeBaseApplicationService.createKnowledgeBase(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/knowledge-bases")
    public ApiResponse<PageResult<KnowledgeBaseResponse>> listKnowledgeBases(
            @PathVariable Long projectId,
            @Valid KnowledgeBaseQueryRequest request) {
        return ApiResponse.success(knowledgeBaseApplicationService.queryKnowledgeBases(projectId, request));
    }

    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseResponse> getKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.success(knowledgeBaseApplicationService.getKnowledgeBase(knowledgeBaseId));
    }

    @PutMapping("/api/knowledge-bases/{knowledgeBaseId}")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeBaseResponse> updateKnowledgeBase(
            @PathVariable Long knowledgeBaseId,
            @Valid @RequestBody KnowledgeBaseUpdateRequest request) {
        return ApiResponse.success(knowledgeBaseApplicationService.updateKnowledgeBase(knowledgeBaseId, request));
    }

    @DeleteMapping("/api/knowledge-bases/{knowledgeBaseId}")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        knowledgeBaseApplicationService.deleteKnowledgeBase(knowledgeBaseId);
        return ApiResponse.success();
    }

    @PostMapping("/api/knowledge-bases/{knowledgeBaseId}/enable")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeBaseResponse> enableKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.success(knowledgeBaseApplicationService.enableKnowledgeBase(knowledgeBaseId));
    }

    @PostMapping("/api/knowledge-bases/{knowledgeBaseId}/disable")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeBaseResponse> disableKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.success(knowledgeBaseApplicationService.disableKnowledgeBase(knowledgeBaseId));
    }

    @PostMapping(value = "/api/knowledge-bases/{knowledgeBaseId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocument(
            @PathVariable Long knowledgeBaseId,
            @Valid @ModelAttribute KnowledgeDocumentUploadRequest request) {
        return ApiResponse.success(knowledgeBaseApplicationService.uploadDocument(knowledgeBaseId, request));
    }

    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<PageResult<KnowledgeDocumentResponse>> listDocuments(
            @PathVariable Long knowledgeBaseId,
            @Valid KnowledgeDocumentQueryRequest request) {
        return ApiResponse.success(knowledgeBaseApplicationService.queryDocuments(knowledgeBaseId, request));
    }

    @GetMapping("/api/knowledge-documents/{documentId}")
    public ApiResponse<KnowledgeDocumentResponse> getDocument(@PathVariable Long documentId) {
        return ApiResponse.success(knowledgeBaseApplicationService.getDocument(documentId));
    }

    @PostMapping("/api/knowledge-documents/{documentId}/index")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeDocumentResponse> indexDocument(@PathVariable Long documentId) {
        return ApiResponse.success(knowledgeBaseApplicationService.createIndexTask(documentId));
    }

    @DeleteMapping("/api/knowledge-documents/{documentId}")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        knowledgeBaseApplicationService.deleteDocument(documentId);
        return ApiResponse.success();
    }
}
