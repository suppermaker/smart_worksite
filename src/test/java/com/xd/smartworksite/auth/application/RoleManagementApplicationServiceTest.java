package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import com.xd.smartworksite.auth.dto.RoleAssignRequest;
import com.xd.smartworksite.auth.dto.RoleCreateRequest;
import com.xd.smartworksite.auth.dto.RoleUpdateRequest;
import com.xd.smartworksite.auth.mapper.RoleMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleManagementApplicationServiceTest {

    private InMemoryRoleMapper roleMapper;
    private RoleManagementApplicationService service;

    @BeforeEach
    void setUp() {
        roleMapper = new InMemoryRoleMapper();
        service = new RoleManagementApplicationService(roleMapper);
        UserPrincipal principal = new UserPrincipal(1L, "admin", List.of("PLATFORM_ADMIN"), List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        roleMapper.roles.add(role(1L, "PLATFORM_ADMIN", "Platform Administrator", "ENABLED"));
        roleMapper.permissions.add(permission(10L, "project:manage", "API"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRoleRejectsDuplicateCode() {
        RoleCreateRequest request = createRequest("platform_admin", "Duplicate");

        assertThatThrownBy(() -> service.createRole(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    @Test
    void createRoleStoresEnabledRoleAndPermissions() {
        RoleCreateRequest request = createRequest("project_auditor", "Project Auditor");
        request.setPermissionIds(List.of(10L));

        var response = service.createRole(request);

        assertThat(response.getRoleCode()).isEqualTo("PROJECT_AUDITOR");
        assertThat(response.getStatus()).isEqualTo("ENABLED");
        assertThat(response.getPermissionIds()).containsExactly(10L);
    }

    @Test
    void assignPermissionsRejectsMissingPermission() {
        roleMapper.roles.add(role(2L, "CUSTOM", "Custom", "ENABLED"));
        RoleAssignRequest request = new RoleAssignRequest();
        request.setPermissionIds(List.of(404L));

        assertThatThrownBy(() -> service.assignPermissions(2L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void updateRoleCanReplacePermissions() {
        Role role = role(2L, "CUSTOM", "Custom", "ENABLED");
        roleMapper.roles.add(role);
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("Custom Updated");
        request.setPermissionIds(List.of(10L));

        var response = service.updateRole(2L, request);

        assertThat(response.getRoleName()).isEqualTo("Custom Updated");
        assertThat(response.getPermissionIds()).containsExactly(10L);
    }

    @Test
    void updateRoleFailsFastWhenRowIsNotUpdated() {
        Role role = role(2L, "CUSTOM", "Custom", "ENABLED");
        roleMapper.roles.add(role);
        roleMapper.failUpdate = true;
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("Custom Updated");

        assertThatThrownBy(() -> service.updateRole(2L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("role update failed");
    }

    @Test
    void assignPermissionsFailsFastWhenPermissionLinkIsNotInserted() {
        Role role = role(2L, "CUSTOM", "Custom", "ENABLED");
        roleMapper.roles.add(role);
        roleMapper.failPermissionInsert = true;
        RoleAssignRequest request = new RoleAssignRequest();
        request.setPermissionIds(List.of(10L));

        assertThatThrownBy(() -> service.assignPermissions(2L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("role permission insert failed");
    }

    @Test
    void builtInRoleCannotBeDisabledOrDeleted() {
        assertThatThrownBy(() -> service.updateStatus(1L, "DISABLED"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
        assertThatThrownBy(() -> service.deleteRole(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    @Test
    void builtInRoleCannotBeUpdatedOrReassignedPermissions() {
        RoleUpdateRequest updateRequest = new RoleUpdateRequest();
        updateRequest.setRoleName("Changed");
        updateRequest.setPermissionIds(List.of(10L));

        assertThatThrownBy(() -> service.updateRole(1L, updateRequest))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));

        RoleAssignRequest assignRequest = new RoleAssignRequest();
        assignRequest.setPermissionIds(List.of(10L));

        assertThatThrownBy(() -> service.assignPermissions(1L, assignRequest))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    @Test
    void deleteRoleRejectsRoleInUse() {
        roleMapper.roles.add(role(2L, "CUSTOM", "Custom", "ENABLED"));
        roleMapper.activeUserCount = 1;

        assertThatThrownBy(() -> service.deleteRole(2L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    private RoleCreateRequest createRequest(String code, String name) {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode(code);
        request.setRoleName(name);
        return request;
    }

    private Role role(Long id, String code, String name, String status) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setStatus(status);
        return role;
    }

    private Permission permission(Long id, String code, String type) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName(code);
        permission.setPermissionType(type);
        return permission;
    }

    private static class InMemoryRoleMapper implements RoleMapper {
        private long nextRoleId = 100L;
        private int activeUserCount = 0;
        private final List<Role> roles = new ArrayList<>();
        private final List<Permission> permissions = new ArrayList<>();
        private final List<Long> rolePermissionIds = new ArrayList<>();
        private boolean failUpdate;
        private boolean failPermissionInsert;

        @Override public List<Role> selectAll(String keyword) { return roles; }
        @Override public Role selectById(Long roleId) { return roles.stream().filter(r -> roleId.equals(r.getId())).findFirst().orElse(null); }
        @Override public Role selectByRoleCode(String roleCode) { return roles.stream().filter(r -> roleCode.equals(r.getRoleCode())).findFirst().orElse(null); }
        @Override public int insert(Role role, Long operatorId) { role.setId(nextRoleId++); roles.add(role); return 1; }
        @Override public int update(Role role, Long operatorId) {
            if (failUpdate) {
                return 0;
            }
            Role stored = selectById(role.getId());
            stored.setRoleName(role.getRoleName());
            stored.setDescription(role.getDescription());
            return 1;
        }
        @Override public int updateStatus(Long roleId, String status, Long operatorId) { selectById(roleId).setStatus(status); return 1; }
        @Override public int softDelete(Long roleId, Long operatorId) { roles.removeIf(r -> roleId.equals(r.getId())); return 1; }
        @Override public int countActiveUsersByRoleId(Long roleId) { return activeUserCount; }
        @Override public List<Permission> selectAllPermissions() { return permissions; }
        @Override public Permission selectPermissionById(Long permissionId) { return permissions.stream().filter(p -> permissionId.equals(p.getId())).findFirst().orElse(null); }
        @Override public List<Long> selectPermissionIdsByRoleId(Long roleId) { return List.copyOf(rolePermissionIds); }
        @Override public int insertRolePermission(Long roleId, Long permissionId, Long operatorId) { if (failPermissionInsert) { return 0; } rolePermissionIds.add(permissionId); return 1; }
        @Override public int deleteRolePermissions(Long roleId) { rolePermissionIds.clear(); return 1; }
        @Override public List<String> selectUserRoleCodes(Long userId) { return List.of(); }
        @Override public int insertUserRole(Long userId, Long roleId, Long operatorId) { return 1; }
        @Override public int deleteUserRoles(Long userId) { return 1; }
    }
}
