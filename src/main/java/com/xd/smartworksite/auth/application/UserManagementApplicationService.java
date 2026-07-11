package com.xd.smartworksite.auth.application;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.auth.domain.Role;
import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.*;
import com.xd.smartworksite.auth.mapper.RoleMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementApplicationService {

    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserManagementApplicationService(UserAccountMapper userAccountMapper,
                                            RoleMapper roleMapper,
                                            PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<UserResponse> queryUsers(UserQueryRequest request) {
        String status = normalizeOptionalStatus(request.getStatus());
        Page<UserAccount> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> userAccountMapper.selectPage(request.getKeyword(), status));
        List<UserResponse> records = page.getResult().stream().map(this::toResponse).toList();
        return new PageResult<>(request.getPageNo(), request.getPageSize(), page.getTotal(), records);
    }

    public UserResponse getUser(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userAccountMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setStatus("ENABLED");
        requireUpdated(userAccountMapper.insert(user), "user create failed");
        if (user.getId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "user create id was not generated");
        }

        assignRoles(user.getId(), request.getRoleCodes());
        return getUser(user.getId());
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        UserAccount user = requireUser(userId);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        requireUpdated(userAccountMapper.update(user), "user update failed");

        if (request.getRoleCodes() != null) {
            assignRoles(userId, request.getRoleCodes());
        }
        return getUser(userId);
    }

    public void changeOwnPassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        requireUpdated(userAccountMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword())), "password update failed");
    }

    public void resetPassword(Long userId, ResetPasswordRequest request) {
        requireUser(userId);
        requireUpdated(userAccountMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword())), "password update failed");
    }

    public void updateStatus(Long userId, String status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改自己的状态");
        }
        requireUser(userId);
        requireUpdated(userAccountMapper.updateStatus(userId, normalizeStatus(status)), "user status update failed");
    }

    private void assignRoles(Long userId, List<String> roleCodes) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        roleMapper.deleteUserRoles(userId);
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                Role role = roleMapper.selectAll(null).stream()
                        .filter(r -> r.getRoleCode().equals(roleCode))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在: " + roleCode));
                requireUpdated(roleMapper.insertUserRole(userId, role.getId(), operatorId), "user role insert failed");
            }
        }
    }

    private void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        return user;
    }

    private UserResponse toResponse(UserAccount user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setDisplayName(user.getDisplayName());
        r.setPhone(user.getPhone());
        r.setEmail(user.getEmail());
        r.setStatus(user.getStatus());
        r.setLastLoginAt(user.getLastLoginAt());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedAt(user.getUpdatedAt());
        r.setRoles(roleMapper.selectUserRoleCodes(user.getId()));
        return r;
    }

    private String trimToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"ENABLED".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
        return normalized;
    }
}
