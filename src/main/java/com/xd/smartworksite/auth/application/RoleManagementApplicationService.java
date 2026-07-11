package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import com.xd.smartworksite.auth.dto.PermissionResponse;
import com.xd.smartworksite.auth.dto.RoleAssignRequest;
import com.xd.smartworksite.auth.dto.RoleCreateRequest;
import com.xd.smartworksite.auth.dto.RoleResponse;
import com.xd.smartworksite.auth.dto.RoleUpdateRequest;
import com.xd.smartworksite.auth.mapper.RoleMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleManagementApplicationService {

    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of(
            "PLATFORM_ADMIN", "PROJECT_ADMIN", "BUSINESS_USER", "VIEWER"
    );

    private final RoleMapper roleMapper;

    public RoleManagementApplicationService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<RoleResponse> listRoles(String keyword) {
        return roleMapper.selectAll(keyword).stream().map(this::toResponse).toList();
    }

    public List<PermissionResponse> listPermissions() {
        return roleMapper.selectAllPermissions().stream().map(this::toPermissionResponse).toList();
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        Role role = new Role();
        role.setRoleCode(normalizeRoleCode(request.getRoleCode()));
        if (roleMapper.selectByRoleCode(role.getRoleCode()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        role.setStatus("ENABLED");
        requireUpdated(roleMapper.insert(role, SecurityUtils.getCurrentUserId()), "role create failed");
        if (role.getId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "role create id was not generated");
        }
        replacePermissions(role.getId(), request.getPermissionIds());
        return toResponse(requireRole(role.getId()));
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, RoleUpdateRequest request) {
        Role role = requireRole(roleId);
        rejectBuiltInRoleChange(role);
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        role.setId(roleId);
        requireUpdated(roleMapper.update(role, SecurityUtils.getCurrentUserId()), "role update failed");
        if (request.getPermissionIds() != null) {
            replacePermissions(roleId, request.getPermissionIds());
        }
        return toResponse(requireRole(roleId));
    }

    @Transactional
    public RoleResponse updateStatus(Long roleId, String status) {
        Role role = requireRole(roleId);
        rejectBuiltInRoleChange(role);
        String normalizedStatus = normalizeStatus(status);
        requireUpdated(roleMapper.updateStatus(roleId, normalizedStatus, SecurityUtils.getCurrentUserId()), "role status update failed");
        return toResponse(requireRole(roleId));
    }

    @Transactional
    public void deleteRole(Long roleId) {
        Role role = requireRole(roleId);
        rejectBuiltInRoleChange(role);
        if (roleMapper.countActiveUsersByRoleId(roleId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色已被用户使用，不能删除");
        }
        roleMapper.deleteRolePermissions(roleId);
        requireUpdated(roleMapper.softDelete(roleId, SecurityUtils.getCurrentUserId()), "role delete failed");
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, RoleAssignRequest request) {
        Role role = requireRole(roleId);
        rejectBuiltInRoleChange(role);
        replacePermissions(roleId, request.getPermissionIds());
        return toResponse(role);
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        return role;
    }

    private void replacePermissions(Long roleId, List<Long> permissionIds) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (permissionIds != null) {
            for (Long permissionId : permissionIds) {
                requirePermission(permissionId);
            }
        }
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null) {
            for (Long permissionId : permissionIds) {
                requireUpdated(roleMapper.insertRolePermission(roleId, permissionId, operatorId), "role permission insert failed");
            }
        }
    }

    private void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }

    private void requirePermission(Long permissionId) {
        if (permissionId == null || roleMapper.selectPermissionById(permissionId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限不存在: " + permissionId);
        }
    }

    private void rejectBuiltInRoleChange(Role role) {
        if (BUILT_IN_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "内置角色不能修改、停用或删除");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ENABLED".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RoleResponse toResponse(Role role) {
        RoleResponse r = new RoleResponse();
        r.setId(role.getId());
        r.setRoleCode(role.getRoleCode());
        r.setRoleName(role.getRoleName());
        r.setDescription(role.getDescription());
        r.setStatus(role.getStatus());
        r.setCreatedAt(role.getCreatedAt());
        r.setUpdatedAt(role.getUpdatedAt());
        r.setPermissionIds(roleMapper.selectPermissionIdsByRoleId(role.getId()));
        return r;
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        PermissionResponse r = new PermissionResponse();
        r.setId(p.getId());
        r.setPermissionCode(p.getPermissionCode());
        r.setPermissionName(p.getPermissionName());
        r.setPermissionType(p.getPermissionType());
        r.setParentId(p.getParentId());
        return r;
    }
}
