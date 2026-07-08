package com.xd.smartworksite.ai.controller;

import com.xd.smartworksite.ai.application.AiApplicationService;
import com.xd.smartworksite.ai.dto.*;
import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {
    private final AiApplicationService aiApplicationService;

    public AiController(AiApplicationService aiApplicationService) {
        this.aiApplicationService = aiApplicationService;
    }

    @PostMapping("/model/invoke")
    public ApiResponse<ModelInvokeResponse> invokeModel(@Valid @RequestBody ModelInvokeRequest request) {
        return ApiResponse.success(aiApplicationService.invokeModel(request));
    }

    @PostMapping("/agent/invoke")
    public ApiResponse<AgentInvokeResponse> invokeAgent(@Valid @RequestBody AgentInvokeRequest request) {
        return ApiResponse.success(aiApplicationService.invokeAgent(request));
    }

    @PostMapping("/knowledge/search")
    public ApiResponse<RagSearchResponse> searchKnowledge(@Valid @RequestBody RagSearchRequest request) {
        return ApiResponse.success(aiApplicationService.searchKnowledge(request));
    }

    @PostMapping("/knowledge/index")
    public ApiResponse<RagIndexResponse> indexKnowledge(@Valid @RequestBody RagIndexRequest request) {
        return ApiResponse.success(aiApplicationService.indexKnowledge(request));
    }

    @PostMapping("/database/query")
    public ApiResponse<DatabaseQueryResponse> queryDatabase(@Valid @RequestBody DatabaseQueryRequest request) {
        return ApiResponse.success(aiApplicationService.queryDatabase(request));
    }

    @PostMapping("/route")
    public ApiResponse<RouteResponse> route(@Valid @RequestBody RouteRequest request) {
        return ApiResponse.success(aiApplicationService.route(request));
    }

    @PostMapping("/context/prepare")
    public ApiResponse<ContextPrepareResponse> prepareContext(@Valid @RequestBody ContextPrepareRequest request) {
        return ApiResponse.success(aiApplicationService.prepareContext(request));
    }

    @GetMapping("/external-call-logs")
    public ApiResponse<PageResult<ExternalCallLogResponse>> queryExternalCallLogs(@Valid ExternalCallLogQueryRequest request) {
        return ApiResponse.success(aiApplicationService.queryExternalCallLogs(request));
    }
}
