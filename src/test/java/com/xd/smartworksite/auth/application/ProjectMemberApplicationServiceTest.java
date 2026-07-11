package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.dto.ProjectMemberCreateRequest;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemberApplicationServiceTest {

    private InMemoryProjectRepository projectRepository;
    private InMemoryProjectMemberMapper projectMemberMapper;
    private InMemoryUserAccountMapper userAccountMapper;
    private ProjectMemberApplicationService service;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        projectMemberMapper = new InMemoryProjectMemberMapper();
        userAccountMapper = new InMemoryUserAccountMapper();
        ProjectAccessApplicationService accessService = new ProjectAccessApplicationService(projectRepository, projectMemberMapper);
        service = new ProjectMemberApplicationService(projectMemberMapper, userAccountMapper, accessService);
        setCurrentUser(1L, List.of("PROJECT_ADMIN"));
        projectRepository.save(project(100L));
        projectMemberMapper.insert(member(100L, 1L, "PROJECT_ADMIN", "ENABLED"));
        userAccountMapper.users.add(user(2L, "worker"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addMemberRejectsMissingUser() {
        ProjectMemberCreateRequest request = request(404L, "MEMBER");

        assertThatThrownBy(() -> service.addMember(100L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode()));
        assertThat(projectMemberMapper.selectByProjectId(100L)).hasSize(1);
    }

    @Test
    void addMemberRejectsDuplicateMembership() {
        userAccountMapper.users.add(user(1L, "admin"));
        ProjectMemberCreateRequest request = request(1L, "MEMBER");

        assertThatThrownBy(() -> service.addMember(100L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
        assertThat(projectMemberMapper.selectByProjectId(100L)).hasSize(1);
    }

    @Test
    void addMemberStoresEnabledMember() {
        ProjectMemberCreateRequest request = request(2L, "SAFETY_OFFICER");

        var response = service.addMember(100L, request);

        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getProjectRole()).isEqualTo("SAFETY_OFFICER");
        assertThat(response.getStatus()).isEqualTo("ENABLED");
    }

    @Test
    void addMemberFailsFastWhenInsertAffectsNoRows() {
        projectMemberMapper.failInsert = true;
        ProjectMemberCreateRequest request = request(2L, "SAFETY_OFFICER");

        assertThatThrownBy(() -> service.addMember(100L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("project member create failed");
    }

    @Test
    void updateMemberFailsFastWhenRowIsNotUpdated() {
        userAccountMapper.users.add(user(1L, "admin"));
        projectMemberMapper.failUpdate = true;
        ProjectMemberCreateRequest request = request(1L, "SAFETY_OFFICER");

        assertThatThrownBy(() -> service.updateMember(100L, 1L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("project member update failed");
    }

    @Test
    void removeMemberRejectsRemovingSelf() {
        assertThatThrownBy(() -> service.removeMember(100L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        assertThat(projectMemberMapper.selectByProjectIdAndUserId(100L, 1L)).isNotNull();
    }

    @Test
    void removeMemberFailsFastWhenRowIsNotDeleted() {
        projectMemberMapper.failDelete = true;
        projectMemberMapper.insert(member(100L, 2L, "PROJECT_USER", "ENABLED"));

        assertThatThrownBy(() -> service.removeMember(100L, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("project member delete failed");
    }

    @Test
    void listMembersRejectsNonMemberAccess() {
        setCurrentUser(9L, List.of());

        assertThatThrownBy(() -> service.listMembers(100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode()));
    }

    private ProjectMemberCreateRequest request(Long userId, String role) {
        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setUserId(userId);
        request.setProjectRole(role);
        return request;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setProjectName("Project " + id);
        project.setProjectCode("P" + id);
        project.setStatus("ENABLED");
        return project;
    }

    private ProjectMember member(Long projectId, Long userId, String role, String status) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setProjectRole(role);
        member.setStatus(status);
        return member;
    }

    private UserAccount user(Long userId, String username) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setStatus("ENABLED");
        return user;
    }

    private void setCurrentUser(Long userId, List<String> roles) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId, roles, List.of(), 100L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final List<Project> projects = new ArrayList<>();
        void save(Project project) { projects.add(project); }
        @Override public List<Project> findPage(String keyword, String status) { return projects; }
        @Override public List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds) { return projects.stream().filter(p -> projectIds.contains(p.getId())).toList(); }
        @Override public Optional<Project> findById(Long projectId) { return projects.stream().filter(p -> projectId.equals(p.getId())).findFirst(); }
        @Override public Optional<Project> findByProjectCode(String projectCode) { return Optional.empty(); }
        @Override public Project insert(Project project) { projects.add(project); return project; }
        @Override public int update(Project project) { return 1; }
        @Override public int softDelete(Long projectId, Long updatedBy) { return 1; }
        @Override public int updateStatus(Long projectId, String status, Long updatedBy) { return 1; }
        @Override public int updateSettings(Long projectId, String settings, Long updatedBy) { return 1; }
        @Override public long countActiveMembers(Long projectId) { return 0; }
        @Override public long countKnowledgeBases(Long projectId) { return 0; }
        @Override public long countReports(Long projectId) { return 0; }
        @Override public long countDataSources(Long projectId) { return 0; }
        @Override public long countQaMessages(Long projectId) { return 0; }
        @Override public long countReviewRecords(Long projectId) { return 0; }
        @Override public long countOcrRecords(Long projectId) { return 0; }
        @Override public long sumFileStorageBytes(Long projectId) { return 0; }
    }

    private static class InMemoryProjectMemberMapper implements ProjectMemberMapper {
        private long nextId = 1L;
        private final List<ProjectMember> members = new ArrayList<>();
        private boolean failInsert;
        private boolean failUpdate;
        private boolean failDelete;
        @Override public List<ProjectMember> selectByProjectId(Long projectId) { return members.stream().filter(m -> projectId.equals(m.getProjectId())).toList(); }
        @Override public ProjectMember selectByProjectIdAndUserId(Long projectId, Long userId) { return members.stream().filter(m -> projectId.equals(m.getProjectId()) && userId.equals(m.getUserId())).findFirst().orElse(null); }
        @Override public int countActiveMember(Long projectId, Long userId) { ProjectMember member = selectByProjectIdAndUserId(projectId, userId); return member != null && "ENABLED".equals(member.getStatus()) ? 1 : 0; }
        @Override public int insert(ProjectMember member) { if (failInsert) { return 0; } member.setId(nextId++); members.add(member); return 1; }
        @Override public int update(ProjectMember member) { return failUpdate ? 0 : 1; }
        @Override public int deleteByProjectIdAndUserId(Long projectId, Long userId, Long operatorId) { if (failDelete) { return 0; } return members.removeIf(m -> projectId.equals(m.getProjectId()) && userId.equals(m.getUserId())) ? 1 : 0; }
        @Override public List<Long> selectProjectIdsByUserId(Long userId) { return members.stream().filter(m -> userId.equals(m.getUserId())).map(ProjectMember::getProjectId).toList(); }
        @Override public List<ProjectMember> selectEnabledByUserId(Long userId) { return members.stream().filter(m -> userId.equals(m.getUserId()) && "ENABLED".equals(m.getStatus())).toList(); }
    }

    private static class InMemoryUserAccountMapper implements UserAccountMapper {
        private final List<UserAccount> users = new ArrayList<>();
        @Override public UserAccount selectByUsername(String username) { return users.stream().filter(u -> username.equals(u.getUsername())).findFirst().orElse(null); }
        @Override public UserAccount selectById(Long userId) { return users.stream().filter(u -> userId.equals(u.getId())).findFirst().orElse(null); }
        @Override public List<UserAccount> selectPage(String keyword, String status) { return users; }
        @Override public int insert(UserAccount user) { users.add(user); return 1; }
        @Override public int update(UserAccount user) { return 1; }
        @Override public int updatePassword(Long userId, String passwordHash) { return 1; }
        @Override public int updateStatus(Long userId, String status) { return 1; }
        @Override public int updateLastLoginAt(Long userId) { return 1; }
        @Override public List<String> selectRoleCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodesByType(Long userId, String permissionType) { return List.of(); }
        @Override public Long selectDefaultProjectId(Long userId) { return null; }
    }
}
