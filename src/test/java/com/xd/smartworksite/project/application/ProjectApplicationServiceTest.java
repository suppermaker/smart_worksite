package com.xd.smartworksite.project.application;

import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.dto.ProjectCreateRequest;
import com.xd.smartworksite.project.dto.ProjectResponse;
import com.xd.smartworksite.project.dto.ProjectUpdateRequest;
import com.xd.smartworksite.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectApplicationServiceTest {

    @BeforeEach
    void setUpSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "test-user", List.of("PLATFORM_ADMIN"), List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void createProjectSucceeds() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        ProjectApplicationService service = new ProjectApplicationService(repository, new InMemoryProjectMemberMapper());

        ProjectResponse response = service.createProject(createRequest("测试项目", "site-001"));

        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getProjectName()).isEqualTo("测试项目");
        assertThat(response.getProjectCode()).isEqualTo("SITE-001");
        assertThat(response.getStatus()).isEqualTo("ENABLED");
    }

    @Test
    void createProjectRejectsDuplicateProjectCode() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        ProjectApplicationService service = new ProjectApplicationService(repository, new InMemoryProjectMemberMapper());
        service.createProject(createRequest("测试项目", "SITE-001"));

        assertThatThrownBy(() -> service.createProject(createRequest("重复项目", "site-001")))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    @Test
    void updateProjectSucceeds() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        ProjectApplicationService service = new ProjectApplicationService(repository, new InMemoryProjectMemberMapper());
        ProjectResponse created = service.createProject(createRequest("测试项目", "SITE-001"));
        ProjectUpdateRequest request = updateRequest("更新后项目", "SITE-002");

        ProjectResponse response = service.updateProject(created.getProjectId(), request);

        assertThat(response.getProjectName()).isEqualTo("更新后项目");
        assertThat(response.getProjectCode()).isEqualTo("SITE-002");
    }

    @Test
    void updateProjectRejectsMissingProject() {
        ProjectApplicationService service = new ProjectApplicationService(new InMemoryProjectRepository(), new InMemoryProjectMemberMapper());

        assertThatThrownBy(() -> service.updateProject(404L, updateRequest("缺失项目", "NO-PROJECT")))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void updateProjectRejectsProjectCodeOwnedByAnotherProject() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        ProjectApplicationService service = new ProjectApplicationService(repository, new InMemoryProjectMemberMapper());
        ProjectResponse first = service.createProject(createRequest("测试项目", "SITE-001"));
        service.createProject(createRequest("项目二", "SITE-002"));

        assertThatThrownBy(() -> service.updateProject(first.getProjectId(), updateRequest("项目一", "SITE-002")))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    private ProjectCreateRequest createRequest(String projectName, String projectCode) {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectName(projectName);
        request.setProjectCode(projectCode);
        request.setLocation("上海");
        request.setDescription("测试描述");
        return request;
    }

    private ProjectUpdateRequest updateRequest(String projectName, String projectCode) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setProjectName(projectName);
        request.setProjectCode(projectCode);
        request.setLocation("上海");
        request.setDescription("更新后的测试描述");
        return request;
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private long nextId = 1L;
        private final List<Project> projects = new ArrayList<>();

        @Override
        public List<Project> findPage(String keyword) {
            return projects.stream()
                    .filter(project -> keyword == null || keyword.isBlank()
                            || project.getProjectName().contains(keyword)
                            || project.getProjectCode().contains(keyword))
                    .toList();
        }

        @Override
        public Optional<Project> findById(Long projectId) {
            return projects.stream().filter(project -> projectId.equals(project.getId())).findFirst();
        }

        @Override
        public Optional<Project> findByProjectCode(String projectCode) {
            return projects.stream().filter(project -> projectCode.equals(project.getProjectCode())).findFirst();
        }

        @Override
        public Project insert(Project project) {
            project.setId(nextId++);
            project.setCreatedAt(LocalDateTime.now());
            project.setUpdatedAt(project.getCreatedAt());
            projects.add(project);
            return project;
        }

        @Override
        public void update(Project project) {
            Project current = findById(project.getId()).orElseThrow();
            current.setProjectName(project.getProjectName());
            current.setProjectCode(project.getProjectCode());
            current.setLocation(project.getLocation());
            current.setDescription(project.getDescription());
            current.setUpdatedAt(LocalDateTime.now());
        }

        @Override
        public void softDelete(Long projectId, Long updatedBy) {
            findById(projectId).ifPresent(project -> project.setUpdatedAt(LocalDateTime.now()));
        }

        @Override
        public void updateStatus(Long projectId, String status, Long updatedBy) {
            Project current = findById(projectId).orElseThrow();
            current.setStatus(status);
            current.setUpdatedAt(LocalDateTime.now());
        }
    }

    private static class InMemoryProjectMemberMapper implements ProjectMemberMapper {
        private final List<ProjectMember> members = new ArrayList<>();

        @Override
        public List<ProjectMember> selectByProjectId(Long projectId) {
            return members.stream().filter(member -> projectId.equals(member.getProjectId())).toList();
        }

        @Override
        public ProjectMember selectByProjectIdAndUserId(Long projectId, Long userId) {
            return members.stream()
                    .filter(member -> projectId.equals(member.getProjectId()) && userId.equals(member.getUserId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int countActiveMember(Long projectId, Long userId) {
            return selectByProjectIdAndUserId(projectId, userId) == null ? 0 : 1;
        }

        @Override
        public int insert(ProjectMember member) {
            members.add(member);
            return 1;
        }

        @Override
        public int update(ProjectMember member) {
            return 1;
        }

        @Override
        public int deleteByProjectIdAndUserId(Long projectId, Long userId, Long operatorId) {
            members.removeIf(member -> projectId.equals(member.getProjectId()) && userId.equals(member.getUserId()));
            return 1;
        }

        @Override
        public List<Long> selectProjectIdsByUserId(Long userId) {
            return members.stream()
                    .filter(member -> userId.equals(member.getUserId()))
                    .map(ProjectMember::getProjectId)
                    .toList();
        }
    }
}
