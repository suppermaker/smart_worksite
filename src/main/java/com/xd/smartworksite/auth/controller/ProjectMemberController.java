package com.xd.smartworksite.auth.controller;

import com.xd.smartworksite.auth.application.ProjectMemberApplicationService;
import com.xd.smartworksite.auth.dto.ProjectMemberCreateRequest;
import com.xd.smartworksite.auth.dto.ProjectMemberResponse;
import com.xd.smartworksite.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@Validated
public class ProjectMemberController {

    private final ProjectMemberApplicationService projectMemberApplicationService;

    public ProjectMemberController(ProjectMemberApplicationService projectMemberApplicationService) {
        this.projectMemberApplicationService = projectMemberApplicationService;
    }

    @GetMapping
    public ApiResponse<List<ProjectMemberResponse>> listMembers(@PathVariable Long projectId) {
        return ApiResponse.success(projectMemberApplicationService.listMembers(projectId));
    }

    @PostMapping
    public ApiResponse<ProjectMemberResponse> addMember(@PathVariable Long projectId,
                                                        @Valid @RequestBody ProjectMemberCreateRequest request) {
        return ApiResponse.success(projectMemberApplicationService.addMember(projectId, request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<ProjectMemberResponse> updateMember(@PathVariable Long projectId,
                                                           @PathVariable Long userId,
                                                           @Valid @RequestBody ProjectMemberCreateRequest request) {
        return ApiResponse.success(projectMemberApplicationService.updateMember(projectId, userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectMemberApplicationService.removeMember(projectId, userId);
        return ApiResponse.success();
    }
}
