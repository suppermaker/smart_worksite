package com.xd.smartworksite.auth.controller;

import com.xd.smartworksite.auth.application.RoleManagementApplicationService;
import com.xd.smartworksite.auth.dto.PermissionResponse;
import com.xd.smartworksite.auth.dto.RoleAssignRequest;
import com.xd.smartworksite.auth.dto.RoleResponse;
import com.xd.smartworksite.common.result.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
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
