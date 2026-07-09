package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.ChangePasswordRequest;
import com.xd.smartworksite.auth.dto.LoginRequest;
import com.xd.smartworksite.auth.dto.LoginResponse;
import com.xd.smartworksite.auth.dto.UserInfoResponse;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.redis.RedisCacheService;
import com.xd.smartworksite.common.redis.RedisKeys;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.JwtTokenService;
import com.xd.smartworksite.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class AuthApplicationService {

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RedisCacheService redisCacheService;

    public AuthApplicationService(UserAccountMapper userAccountMapper,
                                  JwtTokenService jwtTokenService,
                                  PasswordEncoder passwordEncoder,
                                  RedisCacheService redisCacheService) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.redisCacheService = redisCacheService;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountMapper.selectByUsername(request.getUsername());
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        List<String> roles = userAccountMapper.selectRoleCodes(user.getId());
        List<String> permissions = userAccountMapper.selectPermissionCodes(user.getId());
        Long defaultProjectId = userAccountMapper.selectDefaultProjectId(user.getId());

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(),
                roles, permissions, defaultProjectId);
        String token = jwtTokenService.generateToken(principal);

        userAccountMapper.updateLastLoginAt(user.getId());

        UserInfoResponse userInfo = toUserInfo(user, roles, permissions, defaultProjectId);

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
        Long defaultProjectId = userAccountMapper.selectDefaultProjectId(userId);
        return toUserInfo(user, roles, permissions, defaultProjectId);
    }

    public UserInfoResponse toUserInfo(UserAccount user, List<String> roles,
                                       List<String> permissions, Long defaultProjectId) {
        UserInfoResponse info = new UserInfoResponse();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getDisplayName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setStatus(user.getStatus());
        info.setRoles(roles);
        info.setPermissions(permissions);
        info.setDefaultProjectId(defaultProjectId);
        info.setLastLoginAt(user.getLastLoginAt());
        info.setCreatedAt(user.getCreatedAt());
        info.setUpdatedAt(user.getUpdatedAt());
        return info;
    }

    public void changeOwnPassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        userAccountMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    public Claims requireValidToken(String rawToken) {
        return jwtTokenService.parseToken(rawToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "token无效"));
    }
}
