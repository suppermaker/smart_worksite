package com.xd.smartworksite.auth.controller;

import com.xd.smartworksite.auth.application.RoleManagementApplicationService;
import com.xd.smartworksite.auth.dto.PermissionResponse;
import com.xd.smartworksite.auth.dto.RoleAssignRequest;
import com.xd.smartworksite.auth.dto.RoleCreateRequest;
import com.xd.smartworksite.auth.dto.RoleResponse;
import com.xd.smartworksite.auth.dto.RoleUpdateRequest;
import com.xd.smartworksite.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@Validated
public class RoleController {

    private final RoleManagementApplicationService roleManagementApplicationService;

    public RoleController(RoleManagementApplicationService roleManagementApplicationService) {
        this.roleManagementApplicationService = roleManagementApplicationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<RoleResponse>> listRoles(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(roleManagementApplicationService.listRoles(keyword));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleManagementApplicationService.createRole(request));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long roleId,
                                                @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleManagementApplicationService.updateRole(roleId, request));
    }

    @PutMapping("/{roleId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<RoleResponse> updateStatus(@PathVariable Long roleId, @RequestParam String status) {
        return ApiResponse.success(roleManagementApplicationService.updateStatus(roleId, status));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        roleManagementApplicationService.deleteRole(roleId);
        return ApiResponse.success();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(roleManagementApplicationService.listPermissions());
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<RoleResponse> assignPermissions(@PathVariable Long roleId,
                                                       @RequestBody RoleAssignRequest request) {
        return ApiResponse.success(roleManagementApplicationService.assignPermissions(roleId, request));
    }
}
