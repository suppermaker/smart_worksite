package com.xd.smartworksite.intelligence.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.intelligence.application.ModelCallApplicationService;
import com.xd.smartworksite.intelligence.dto.ModelCallRequest;
import com.xd.smartworksite.intelligence.dto.ModelCallResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence")
@Validated
public class ModelCallController {

    private final ModelCallApplicationService modelCallApplicationService;

    public ModelCallController(ModelCallApplicationService modelCallApplicationService) {
        this.modelCallApplicationService = modelCallApplicationService;
    }

    @PostMapping("/model/call")
    public ApiResponse<ModelCallResponse> call(@Valid @RequestBody ModelCallRequest request) {
        return ApiResponse.success(modelCallApplicationService.call(request));
    }
}
