package com.xd.smartworksite.project.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.project.application.ProjectApplicationService;
import com.xd.smartworksite.project.dto.ProjectCreateRequest;
import com.xd.smartworksite.project.dto.ProjectQueryRequest;
import com.xd.smartworksite.project.dto.ProjectResponse;
import com.xd.smartworksite.project.dto.ProjectSettingsRequest;
import com.xd.smartworksite.project.dto.ProjectSettingsResponse;
import com.xd.smartworksite.project.dto.ProjectStatusRequest;
import com.xd.smartworksite.project.dto.ProjectStatisticsResponse;
import com.xd.smartworksite.project.dto.ProjectUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {

    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectResponse>> listProjects(@Valid ProjectQueryRequest request) {
        return ApiResponse.success(projectApplicationService.queryProjects(request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(projectApplicationService.getProject(projectId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectApplicationService.createProject(request));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<ProjectResponse> updateProject(@PathVariable Long projectId,
                                                      @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.success(projectApplicationService.updateProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<Void> deleteProject(@PathVariable Long projectId) {
        projectApplicationService.deleteProject(projectId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{projectId}/status")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<Void> updateProjectStatus(@PathVariable Long projectId,
                                                 @Valid @RequestBody ProjectStatusRequest request) {
        projectApplicationService.updateProjectStatus(projectId, request.getStatus());
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/enable")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<Void> enableProject(@PathVariable Long projectId) {
        projectApplicationService.updateProjectStatus(projectId, "ENABLED");
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/disable")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<Void> disableProject(@PathVariable Long projectId) {
        projectApplicationService.updateProjectStatus(projectId, "DISABLED");
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/archive")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<Void> archiveProject(@PathVariable Long projectId) {
        projectApplicationService.updateProjectStatus(projectId, "ARCHIVED");
        return ApiResponse.success(null);
    }

    @GetMapping("/{projectId}/settings")
    public ApiResponse<ProjectSettingsResponse> getProjectSettings(@PathVariable Long projectId) {
        return ApiResponse.success(projectApplicationService.getProjectSettings(projectId));
    }

    @PutMapping("/{projectId}/settings")
    @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<ProjectSettingsResponse> updateProjectSettings(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSettingsRequest request) {
        return ApiResponse.success(projectApplicationService.updateProjectSettings(projectId, request));
    }

    @GetMapping("/{projectId}/statistics")
    public ApiResponse<ProjectStatisticsResponse> getProjectStatistics(@PathVariable Long projectId) {
        return ApiResponse.success(projectApplicationService.getProjectStatistics(projectId));
    }
}
