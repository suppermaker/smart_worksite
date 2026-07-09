package com.xd.smartworksite.auth.controller;

import com.xd.smartworksite.auth.application.UserManagementApplicationService;
import com.xd.smartworksite.auth.dto.*;
import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/users")
@Validated
public class UserController {

    private final UserManagementApplicationService userManagementApplicationService;

    public UserController(UserManagementApplicationService userManagementApplicationService) {
        this.userManagementApplicationService = userManagementApplicationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResult<UserResponse>> listUsers(@Valid UserQueryRequest request) {
        return ApiResponse.success(userManagementApplicationService.queryUsers(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userManagementApplicationService.createUser(request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userManagementApplicationService.getUser(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userId,
                                                @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userManagementApplicationService.updateUser(userId, request));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> updateStatus(@PathVariable Long userId, @RequestParam String status) {
        userManagementApplicationService.updateStatus(userId, status);
        return ApiResponse.success();
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userManagementApplicationService.resetPassword(userId, request);
        return ApiResponse.success();
    }
}
