package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.ChangePasswordRequest;
import com.xd.smartworksite.auth.dto.LoginRequest;
import com.xd.smartworksite.auth.dto.LoginResponse;
import com.xd.smartworksite.auth.dto.UserInfoResponse;
import com.xd.smartworksite.auth.dto.UserProjectResponse;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.redis.RedisCacheService;
import com.xd.smartworksite.common.redis.RedisKeys;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.JwtTokenService;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthApplicationService {

    private final UserAccountMapper userAccountMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectRepository projectRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RedisCacheService redisCacheService;
    private final LoginSecurityProperties loginSecurityProperties;

    public AuthApplicationService(UserAccountMapper userAccountMapper,
                                  ProjectMemberMapper projectMemberMapper,
                                  ProjectRepository projectRepository,
                                  JwtTokenService jwtTokenService,
                                  PasswordEncoder passwordEncoder,
                                  RedisCacheService redisCacheService,
                                  LoginSecurityProperties loginSecurityProperties) {
        this.userAccountMapper = userAccountMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.projectRepository = projectRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.redisCacheService = redisCacheService;
        this.loginSecurityProperties = loginSecurityProperties;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        rejectIfLoginLocked(username);

        UserAccount user = userAccountMapper.selectByUsername(username);
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            recordLoginFailure(username);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(username);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        List<String> roles = userAccountMapper.selectRoleCodes(user.getId());
        List<String> permissions = userAccountMapper.selectPermissionCodes(user.getId());
        List<String> buttonPermissions = userAccountMapper.selectPermissionCodesByType(user.getId(), "BUTTON");
        Long defaultProjectId = userAccountMapper.selectDefaultProjectId(user.getId());

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(),
                roles, permissions, defaultProjectId);
        String token = jwtTokenService.generateToken(principal);

        clearLoginFailure(username);
        requireUpdated(userAccountMapper.updateLastLoginAt(user.getId()), "last login time update failed");

        UserInfoResponse userInfo = toUserInfo(user, roles, permissions, buttonPermissions, defaultProjectId);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setExpiresIn(jwtTokenService.getExpiresInSeconds());
        response.setUser(userInfo);
        return response;
    }

    public void logout(String rawToken) {
        jwtTokenService.parseToken(rawToken).ifPresent(claims -> {
            String jti = claims.getId();
            if (jti != null) {
                Date exp = claims.getExpiration();
                long remainingSeconds = exp == null ? 3600L
                        : Math.max(0, (exp.toInstant().getEpochSecond() - Instant.now().getEpochSecond()));
                redisCacheService.set(RedisKeys.tokenBlacklist(jti), "1", Duration.ofSeconds(remainingSeconds));
            }
        });
    }

    public UserInfoResponse getCurrentUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        List<String> roles = userAccountMapper.selectRoleCodes(userId);
        List<String> permissions = userAccountMapper.selectPermissionCodes(userId);
        List<String> buttonPermissions = userAccountMapper.selectPermissionCodesByType(userId, "BUTTON");
        Long defaultProjectId = userAccountMapper.selectDefaultProjectId(userId);
        return toUserInfo(user, roles, permissions, buttonPermissions, defaultProjectId);
    }

    public UserInfoResponse toUserInfo(UserAccount user, List<String> roles,
                                       List<String> permissions, List<String> buttonPermissions,
                                       Long defaultProjectId) {
        UserInfoResponse info = new UserInfoResponse();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getDisplayName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setStatus(user.getStatus());
        info.setRoles(roles);
        info.setPermissions(permissions);
        info.setButtonPermissions(buttonPermissions);
        info.setProjects(loadUserProjects(user.getId(), roles));
        info.setDefaultProjectId(defaultProjectId);
        info.setLastLoginAt(user.getLastLoginAt());
        info.setCreatedAt(user.getCreatedAt());
        info.setUpdatedAt(user.getUpdatedAt());
        return info;
    }

    private List<UserProjectResponse> loadUserProjects(Long userId, List<String> roles) {
        if (roles.contains("PLATFORM_ADMIN")) {
            return projectRepository.findPage(null, null).stream()
                    .map(project -> toUserProjectResponse(project, "PLATFORM_ADMIN"))
                    .toList();
        }
        List<ProjectMember> memberships = projectMemberMapper.selectEnabledByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = memberships.stream().map(ProjectMember::getProjectId).toList();
        Map<Long, ProjectMember> memberByProjectId = memberships.stream()
                .collect(Collectors.toMap(ProjectMember::getProjectId, Function.identity(), (left, right) -> left));
        return projectRepository.findPageByProjectIds(null, null, projectIds).stream()
                .map(project -> toUserProjectResponse(project, memberByProjectId.get(project.getId()).getProjectRole()))
                .toList();
    }

    private UserProjectResponse toUserProjectResponse(Project project, String projectRole) {
        UserProjectResponse response = new UserProjectResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setProjectCode(project.getProjectCode());
        response.setStatus(project.getStatus());
        response.setProjectRole(projectRole);
        return response;
    }

    public void changeOwnPassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        requireUpdated(userAccountMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword())), "password update failed");
    }

    public Claims requireValidToken(String rawToken) {
        return jwtTokenService.parseToken(rawToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "token无效"));
    }

    private void rejectIfLoginLocked(String username) {
        if (redisCacheService.get(RedisKeys.loginLock(username)).isPresent()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "账号已临时锁定，请稍后再试");
        }
    }

    private void recordLoginFailure(String username) {
        String failureKey = RedisKeys.loginFailureCount(username);
        int failures = redisCacheService.get(failureKey)
                .map(this::parseFailureCount)
                .orElse(0) + 1;
        redisCacheService.set(failureKey, String.valueOf(failures),
                Duration.ofSeconds(loginSecurityProperties.getFailureWindowSeconds()));
        if (failures >= loginSecurityProperties.getMaxFailureCount()) {
            redisCacheService.set(RedisKeys.loginLock(username), "1",
                    Duration.ofSeconds(loginSecurityProperties.getLockSeconds()));
        }
    }

    private int parseFailureCount(String rawCount) {
        try {
            return Integer.parseInt(rawCount);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "login failure counter is corrupted");
        }
    }

    private void clearLoginFailure(String username) {
        redisCacheService.delete(RedisKeys.loginFailureCount(username));
        redisCacheService.delete(RedisKeys.loginLock(username));
    }

    private void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }
}
