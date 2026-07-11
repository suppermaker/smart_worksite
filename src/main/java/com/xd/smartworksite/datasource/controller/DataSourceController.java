package com.xd.smartworksite.datasource.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.datasource.application.DataSourceApplicationService;
import com.xd.smartworksite.datasource.dto.DataSourceConnectionTestResponse;
import com.xd.smartworksite.datasource.dto.DataSourceCreateRequest;
import com.xd.smartworksite.datasource.dto.DataSourceQueryRequest;
import com.xd.smartworksite.datasource.dto.DataSourceResponse;
import com.xd.smartworksite.datasource.dto.DataSourceSchemaResponse;
import com.xd.smartworksite.datasource.dto.DataSourceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-sources")
@Validated
public class DataSourceController {
    private final DataSourceApplicationService dataSourceApplicationService;

    public DataSourceController(DataSourceApplicationService dataSourceApplicationService) {
        this.dataSourceApplicationService = dataSourceApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('datasource:manage')")
    public ApiResponse<DataSourceResponse> createDataSource(@Valid @RequestBody DataSourceCreateRequest request) {
        return ApiResponse.success(dataSourceApplicationService.createDataSource(request));
    }

    @GetMapping
    public ApiResponse<PageResult<DataSourceResponse>> listDataSources(@Valid DataSourceQueryRequest request) {
        return ApiResponse.success(dataSourceApplicationService.queryDataSources(request));
    }

    @GetMapping("/{dataSourceId}")
    public ApiResponse<DataSourceResponse> getDataSource(@PathVariable Long dataSourceId) {
        return ApiResponse.success(dataSourceApplicationService.getDataSource(dataSourceId));
    }

    @PostMapping("/{dataSourceId}/test")
    public ApiResponse<DataSourceConnectionTestResponse> testConnection(@PathVariable Long dataSourceId) {
        return ApiResponse.success(dataSourceApplicationService.testConnection(dataSourceId));
    }

    @GetMapping("/{dataSourceId}/schema")
    public ApiResponse<DataSourceSchemaResponse> inspectSchema(@PathVariable Long dataSourceId) {
        return ApiResponse.success(dataSourceApplicationService.inspectSchema(dataSourceId));
    }

    @PutMapping("/{dataSourceId}")
    @PreAuthorize("hasAuthority('datasource:manage')")
    public ApiResponse<DataSourceResponse> updateDataSource(
            @PathVariable Long dataSourceId,
            @Valid @RequestBody DataSourceUpdateRequest request) {
        return ApiResponse.success(dataSourceApplicationService.updateDataSource(dataSourceId, request));
    }

    @PostMapping("/{dataSourceId}/enable")
    @PreAuthorize("hasAuthority('datasource:manage')")
    public ApiResponse<DataSourceResponse> enableDataSource(@PathVariable Long dataSourceId) {
        return ApiResponse.success(dataSourceApplicationService.enableDataSource(dataSourceId));
    }

    @PostMapping("/{dataSourceId}/disable")
    @PreAuthorize("hasAuthority('datasource:manage')")
    public ApiResponse<DataSourceResponse> disableDataSource(@PathVariable Long dataSourceId) {
        return ApiResponse.success(dataSourceApplicationService.disableDataSource(dataSourceId));
    }

    @DeleteMapping("/{dataSourceId}")
    @PreAuthorize("hasAuthority('datasource:manage')")
    public ApiResponse<Void> deleteDataSource(@PathVariable Long dataSourceId) {
        dataSourceApplicationService.deleteDataSource(dataSourceId);
        return ApiResponse.success();
    }
}
