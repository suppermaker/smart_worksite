package com.xd.smartworksite.system.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.system.application.SystemStatusApplicationService;
import com.xd.smartworksite.system.dto.SystemDependencyHealthResponse;
import com.xd.smartworksite.system.dto.SystemRuntimeResponse;
import com.xd.smartworksite.system.dto.SystemVersionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemStatusApplicationService systemStatusApplicationService;

    public SystemController(SystemStatusApplicationService systemStatusApplicationService) {
        this.systemStatusApplicationService = systemStatusApplicationService;
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "smart-worksite",
                "time", OffsetDateTime.now()
        ));
    }

    @GetMapping("/version")
    public ApiResponse<SystemVersionResponse> version() {
        return ApiResponse.success(systemStatusApplicationService.version());
    }

    @GetMapping("/runtime")
    public ApiResponse<SystemRuntimeResponse> runtime() {
        return ApiResponse.success(systemStatusApplicationService.runtime());
    }

    @GetMapping("/dependencies/health")
    public ApiResponse<SystemDependencyHealthResponse> dependenciesHealth() {
        return ApiResponse.success(systemStatusApplicationService.dependenciesHealth());
    }
}
