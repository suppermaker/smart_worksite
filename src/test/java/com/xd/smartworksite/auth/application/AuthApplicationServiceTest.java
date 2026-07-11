package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.LoginRequest;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.redis.RedisCacheService;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.JwtTokenService;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {

    private InMemoryUserAccountMapper userAccountMapper;
    private InMemoryRedisCacheService redisCacheService;
    private ProjectMemberMapper projectMemberMapper;
    private ProjectRepository projectRepository;
    private PasswordEncoder passwordEncoder;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        userAccountMapper = new InMemoryUserAccountMapper();
        redisCacheService = new InMemoryRedisCacheService();
        projectMemberMapper = mock(ProjectMemberMapper.class);
        projectRepository = mock(ProjectRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        LoginSecurityProperties properties = new LoginSecurityProperties();
        properties.setMaxFailureCount(3);
        properties.setFailureWindowSeconds(60);
        properties.setLockSeconds(120);
        service = new AuthApplicationService(userAccountMapper, projectMemberMapper, projectRepository,
                mock(JwtTokenService.class), passwordEncoder, redisCacheService, properties);

        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername("admin");
        user.setDisplayName("Admin");
        user.setStatus("ENABLED");
        user.setPasswordHash(passwordEncoder.encode("right-password"));
        userAccountMapper.users.put("admin", user);
        when(projectRepository.findPage(null, null)).thenReturn(List.of(project(100L)));
    }

    @Test
    void loginLocksAccountAfterConfiguredFailures() {
        LoginRequest request = login("admin", "wrong-password");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode()));
        }

        assertThatThrownBy(() -> service.login(login("admin", "right-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS.getCode()));
    }

    @Test
    void successfulLoginClearsFailureCounter() {
        assertThatThrownBy(() -> service.login(login("admin", "wrong-password")))
                .isInstanceOf(BusinessException.class);

        service.login(login("admin", "right-password"));

        assertThat(redisCacheService.keys()).noneMatch(key -> key.contains(":auth:login-failure:admin"));
        assertThat(redisCacheService.keys()).noneMatch(key -> key.contains(":auth:login-lock:admin"));
    }

    @Test
    void loginFailsFastWhenLastLoginTimeCannotBePersisted() {
        userAccountMapper.failLastLoginUpdate = true;

        assertThatThrownBy(() -> service.login(login("admin", "right-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("last login time update failed");
    }

    @Test
    void currentUserInfoIncludesProjectsAndButtonPermissions() {
        userAccountMapper.buttonPermissions = List.of("project:create");

        var response = service.login(login("admin", "right-password")).getUser();

        assertThat(response.getButtonPermissions()).containsExactly("project:create");
        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getProjects().get(0).getProjectId()).isEqualTo(100L);
        assertThat(response.getProjects().get(0).getProjectRole()).isEqualTo("PLATFORM_ADMIN");
    }

    @Test
    void corruptedFailureCounterFailsFast() {
        redisCacheService.set("smart-worksite:auth:login-failure:admin", "not-a-number", Duration.ofSeconds(60));

        assertThatThrownBy(() -> service.login(login("admin", "wrong-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode()));
    }

    private LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private Project project(Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setProjectName("Project " + projectId);
        project.setProjectCode("P" + projectId);
        project.setStatus("ENABLED");
        return project;
    }

    private static class InMemoryRedisCacheService extends RedisCacheService {
        private final Map<String, String> values = new HashMap<>();

        InMemoryRedisCacheService() {
            super(mock(StringRedisTemplate.class));
        }

        @Override public void set(String key, String value, Duration ttl) { values.put(key, value); }
        @Override public java.util.Optional<String> get(String key) { return java.util.Optional.ofNullable(values.get(key)); }
        @Override public void delete(String key) { values.remove(key); }
        java.util.Set<String> keys() { return values.keySet(); }
    }

    private static class InMemoryUserAccountMapper implements UserAccountMapper {
        private final Map<String, UserAccount> users = new HashMap<>();
        private List<String> buttonPermissions = List.of();
        private boolean failLastLoginUpdate;
        @Override public UserAccount selectByUsername(String username) { return users.get(username); }
        @Override public UserAccount selectById(Long userId) { return users.values().stream().filter(u -> userId.equals(u.getId())).findFirst().orElse(null); }
        @Override public List<UserAccount> selectPage(String keyword, String status) { return List.copyOf(users.values()); }
        @Override public int insert(UserAccount user) { users.put(user.getUsername(), user); return 1; }
        @Override public int update(UserAccount user) { users.put(user.getUsername(), user); return 1; }
        @Override public int updatePassword(Long userId, String passwordHash) { return 1; }
        @Override public int updateStatus(Long userId, String status) { return 1; }
        @Override public int updateLastLoginAt(Long userId) { return failLastLoginUpdate ? 0 : 1; }
        @Override public List<String> selectRoleCodes(Long userId) { return List.of("PLATFORM_ADMIN"); }
        @Override public List<String> selectPermissionCodes(Long userId) { return List.of("project:manage"); }
        @Override public List<String> selectPermissionCodesByType(Long userId, String permissionType) { return buttonPermissions; }
        @Override public Long selectDefaultProjectId(Long userId) { return 100L; }
    }
}
