package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.UserQueryRequest;
import com.xd.smartworksite.auth.mapper.RoleMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserManagementApplicationServiceTest {
    private InMemoryUserAccountMapper userMapper;
    private UserManagementApplicationService service;

    @BeforeEach
    void setUp() {
        setCurrentUser(1L);
        userMapper = new InMemoryUserAccountMapper();
        userMapper.users.add(user(2L, "target", "ENABLED"));
        service = new UserManagementApplicationService(userMapper, new EmptyRoleMapper(), new PlainPasswordEncoder());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void queryUsersRejectsInvalidStatus() {
        UserQueryRequest request = new UserQueryRequest();
        request.setStatus("UNKNOWN");

        assertThatThrownBy(() -> service.queryUsers(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void updateStatusRejectsInvalidStatusWithoutPersistingDirtyValue() {
        assertThatThrownBy(() -> service.updateStatus(2L, "UNKNOWN"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));

        assertThat(userMapper.users.get(0).getStatus()).isEqualTo("ENABLED");
    }

    @Test
    void updateStatusNormalizesValidStatus() {
        service.updateStatus(2L, " disabled ");

        assertThat(userMapper.users.get(0).getStatus()).isEqualTo("DISABLED");
    }

    @Test
    void updateStatusFailsFastWhenRowIsNotUpdated() {
        userMapper.failStatusUpdate = true;

        assertThatThrownBy(() -> service.updateStatus(2L, "DISABLED"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("user status update failed");
    }

    private void setCurrentUser(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId, List.of("PLATFORM_ADMIN"), List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private UserAccount user(Long id, String username, String status) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setPasswordHash("password");
        user.setStatus(status);
        return user;
    }

    private static class InMemoryUserAccountMapper implements UserAccountMapper {
        private final List<UserAccount> users = new ArrayList<>();
        private boolean failStatusUpdate;

        @Override public UserAccount selectByUsername(String username) {
            return users.stream().filter(user -> username.equals(user.getUsername())).findFirst().orElse(null);
        }
        @Override public UserAccount selectById(Long userId) {
            return users.stream().filter(user -> userId.equals(user.getId())).findFirst().orElse(null);
        }
        @Override public List<UserAccount> selectPage(String keyword, String status) {
            return users.stream()
                    .filter(user -> status == null || status.equals(user.getStatus()))
                    .toList();
        }
        @Override public int insert(UserAccount user) { users.add(user); return 1; }
        @Override public int update(UserAccount user) { return 1; }
        @Override public int updatePassword(Long userId, String passwordHash) { return 1; }
        @Override public int updateStatus(Long userId, String status) {
            if (failStatusUpdate) {
                return 0;
            }
            UserAccount user = selectById(userId);
            if (user == null) {
                return 0;
            }
            user.setStatus(status);
            return 1;
        }
        @Override public int updateLastLoginAt(Long userId) { return 1; }
        @Override public List<String> selectRoleCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodesByType(Long userId, String permissionType) { return List.of(); }
        @Override public Long selectDefaultProjectId(Long userId) { return null; }
    }

    private static class EmptyRoleMapper implements RoleMapper {
        @Override public List<Role> selectAll(String keyword) { return List.of(); }
        @Override public Role selectById(Long roleId) { return null; }
        @Override public Role selectByRoleCode(String roleCode) { return null; }
        @Override public int insert(Role role, Long operatorId) { return 1; }
        @Override public int update(Role role, Long operatorId) { return 1; }
        @Override public int updateStatus(Long roleId, String status, Long operatorId) { return 1; }
        @Override public int softDelete(Long roleId, Long operatorId) { return 1; }
        @Override public int countActiveUsersByRoleId(Long roleId) { return 0; }
        @Override public List<Permission> selectAllPermissions() { return List.of(); }
        @Override public Permission selectPermissionById(Long permissionId) { return null; }
        @Override public List<Long> selectPermissionIdsByRoleId(Long roleId) { return List.of(); }
        @Override public int insertRolePermission(Long roleId, Long permissionId, Long operatorId) { return 1; }
        @Override public int deleteRolePermissions(Long roleId) { return 1; }
        @Override public List<String> selectUserRoleCodes(Long userId) { return List.of(); }
        @Override public int insertUserRole(Long userId, Long roleId, Long operatorId) { return 1; }
        @Override public int deleteUserRoles(Long userId) { return 1; }
    }

    private static class PlainPasswordEncoder implements PasswordEncoder {
        @Override public String encode(CharSequence rawPassword) { return rawPassword.toString(); }
        @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    }
}
