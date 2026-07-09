package com.xd.smartworksite.auth.controller;

import com.xd.smartworksite.auth.application.AuthApplicationService;
import com.xd.smartworksite.auth.dto.ChangePasswordRequest;
import com.xd.smartworksite.auth.dto.LoginRequest;
import com.xd.smartworksite.auth.dto.LoginResponse;
import com.xd.smartworksite.auth.dto.UserInfoResponse;
import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authApplicationService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authApplicationService.logout(header.substring(7));
        }
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(authApplicationService.getCurrentUser(principal.getUserId()));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authApplicationService.changeOwnPassword(principal.getUserId(), request);
        return ApiResponse.success();
    }
}
