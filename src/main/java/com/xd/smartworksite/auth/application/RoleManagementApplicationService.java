package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import com.xd.smartworksite.auth.dto.PermissionResponse;
import com.xd.smartworksite.auth.dto.RoleAssignRequest;
import com.xd.smartworksite.auth.dto.RoleResponse;
import com.xd.smartworksite.auth.mapper.RoleMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleManagementApplicationService {

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
    public RoleResponse assignPermissions(Long roleId, RoleAssignRequest request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");

        Long operatorId = SecurityUtils.getCurrentUserId();
        roleMapper.deleteRolePermissions(roleId);
        if (request.getPermissionIds() != null) {
            for (Long permissionId : request.getPermissionIds()) {
                roleMapper.insertRolePermission(roleId, permissionId, operatorId);
            }
        }
        return toResponse(role);
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
