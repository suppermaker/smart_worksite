package com.xd.smartworksite.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.ai.domain.DataSourceRecord;
import com.xd.smartworksite.ai.domain.ExternalCallLog;
import com.xd.smartworksite.ai.dto.ExternalCallLogQueryRequest;
import com.xd.smartworksite.ai.infra.AiPythonServiceClient;
import com.xd.smartworksite.ai.infra.AiPythonServiceProperties;
import com.xd.smartworksite.ai.infra.SafeSqlExecutor;
import com.xd.smartworksite.ai.repository.AiRepository;
import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AiApplicationServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformAdminQueryExternalCallLogsWithoutProjectDoesNotApplyMemberProjectFilter() {
        setCurrentUser(1L, List.of("PLATFORM_ADMIN"));
        InMemoryAiRepository aiRepository = new InMemoryAiRepository();
        AiApplicationService service = new AiApplicationService(
                new AiPythonServiceProperties(),
                new AiPythonServiceClient(new AiPythonServiceProperties(), new ObjectMapper(), aiRepository),
                aiRepository,
                null,
                new ProjectAccessApplicationService(new InMemoryProjectRepository(), new EmptyProjectMemberMapper())
        );

        service.queryExternalCallLogs(new ExternalCallLogQueryRequest());

        assertThat(aiRepository.lastAccessibleProjectIds).isNull();
    }

    private void setCurrentUser(Long userId, List<String> roles) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId, roles, List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private static class InMemoryAiRepository implements AiRepository {
        private List<Long> lastAccessibleProjectIds;

        @Override public int saveExternalCallLog(ExternalCallLog log) { log.setId(1L); return 1; }

        @Override
        public List<ExternalCallLog> queryExternalCallLogs(Long projectId, List<Long> accessibleProjectIds,
                                                           String serviceName, String callType, String status) {
            lastAccessibleProjectIds = accessibleProjectIds;
            return List.of();
        }

        @Override public DataSourceRecord findEnabledDataSource(Long projectId, Long dataSourceId) { return null; }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        @Override public List<Project> findPage(String keyword, String status) { return List.of(); }
        @Override public List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds) { return List.of(); }
        @Override public Optional<Project> findById(Long projectId) {
            Project project = new Project();
            project.setId(projectId);
            project.setStatus("ENABLED");
            return Optional.of(project);
        }
        @Override public Optional<Project> findByProjectCode(String projectCode) { return Optional.empty(); }
        @Override public Project insert(Project project) { return project; }
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

    private static class EmptyProjectMemberMapper implements ProjectMemberMapper {
        @Override public List<ProjectMember> selectByProjectId(Long projectId) { return List.of(); }
        @Override public ProjectMember selectByProjectIdAndUserId(Long projectId, Long userId) { return null; }
        @Override public int countActiveMember(Long projectId, Long userId) { return 0; }
        @Override public int insert(ProjectMember member) { return 1; }
        @Override public int update(ProjectMember member) { return 1; }
        @Override public int deleteByProjectIdAndUserId(Long projectId, Long userId, Long operatorId) { return 1; }
        @Override public List<Long> selectProjectIdsByUserId(Long userId) { return new ArrayList<>(); }
        @Override public List<ProjectMember> selectEnabledByUserId(Long userId) { return List.of(); }
    }
}
