package com.xd.smartworksite.audit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.audit.domain.AuditLog;
import com.xd.smartworksite.audit.dto.AuditLogQueryRequest;
import com.xd.smartworksite.audit.repository.AuditLogRepository;
import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.common.config.RequestContext;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditApplicationServiceTest {
    private InMemoryAuditLogRepository auditLogRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryProjectMemberMapper memberMapper;
    private AuditApplicationService service;

    @BeforeEach
    void setUp() {
        setCurrentUser(2L, List.of("PROJECT_USER"));
        auditLogRepository = new InMemoryAuditLogRepository();
        projectRepository = new InMemoryProjectRepository();
        memberMapper = new InMemoryProjectMemberMapper();
        projectRepository.insert(project(1L));
        projectRepository.insert(project(2L));
        memberMapper.insert(member(1L, 2L, "PROJECT_USER", "ENABLED"));
        service = new AuditApplicationService(
                auditLogRepository,
                new ProjectAccessApplicationService(projectRepository, memberMapper),
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void recordStoresOperatorRequestAndDetail() {
        MDC.put(RequestContext.REQUEST_ID_MDC_KEY, "req-1");

        service.record(1L, "PROJECT_CREATE", "PROJECT", 1L, java.util.Map.of("name", "项目一"));

        AuditLog log = auditLogRepository.records.get(0);
        assertThat(log.getOperatorId()).isEqualTo(2L);
        assertThat(log.getRequestId()).isEqualTo("req-1");
        assertThat(log.getDetail()).contains("项目一");
    }

    @Test
    void recordFailsFastWhenAuditLogCannotBePersisted() {
        auditLogRepository.failInsert = true;

        assertThatThrownBy(() -> service.record(1L, "PROJECT_CREATE", "PROJECT", 1L, java.util.Map.of()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("audit log insert failed");
    }

    @Test
    void queryAuditLogsFiltersToAccessibleProjects() {
        auditLogRepository.insert(log(1L, 2L, "PROJECT_CREATE"));
        auditLogRepository.insert(log(2L, 3L, "PROJECT_DELETE"));

        var result = service.queryAuditLogs(new AuditLogQueryRequest());

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void queryAuditLogsRejectsForbiddenProject() {
        AuditLogQueryRequest request = new AuditLogQueryRequest();
        request.setProjectId(2L);

        assertThatThrownBy(() -> service.queryAuditLogs(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void platformAdminQueryAuditLogsWithoutProjectDoesNotApplyMemberProjectFilter() {
        setCurrentUser(1L, List.of("PLATFORM_ADMIN"));
        auditLogRepository.insert(log(1L, 2L, "PROJECT_CREATE"));

        service.queryAuditLogs(new AuditLogQueryRequest());

        assertThat(auditLogRepository.lastAccessibleProjectIds).isNull();
    }

    private AuditLog log(Long projectId, Long operatorId, String action) {
        AuditLog log = new AuditLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setObjectType("PROJECT");
        log.setObjectId(projectId);
        log.setDetail("{}");
        return log;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setProjectName("项目" + id);
        project.setProjectCode("SITE-" + id);
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

    private void setCurrentUser(Long userId, List<String> roles) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId, roles, List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private static class InMemoryAuditLogRepository implements AuditLogRepository {
        private long nextId = 1L;
        private final List<AuditLog> records = new ArrayList<>();
        private List<Long> lastAccessibleProjectIds;
        private boolean failInsert;

        @Override
        public int insert(AuditLog log) {
            if (failInsert) {
                return 0;
            }
            log.setId(nextId++);
            log.setCreatedAt(LocalDateTime.now());
            records.add(log);
            return 1;
        }

        @Override
        public List<AuditLog> findPage(Long projectId, List<Long> accessibleProjectIds, Long operatorId,
                                       String action, String objectType, LocalDateTime createdFrom, LocalDateTime createdTo) {
            lastAccessibleProjectIds = accessibleProjectIds;
            return records.stream()
                    .filter(log -> projectId == null || projectId.equals(log.getProjectId()))
                    .filter(log -> accessibleProjectIds == null || accessibleProjectIds.contains(log.getProjectId()))
                    .filter(log -> operatorId == null || operatorId.equals(log.getOperatorId()))
                    .filter(log -> action == null || action.equals(log.getAction()))
                    .filter(log -> objectType == null || objectType.equals(log.getObjectType()))
                    .toList();
        }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final List<Project> projects = new ArrayList<>();

        @Override public List<Project> findPage(String keyword, String status) { return projects; }
        @Override public List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds) {
            return projects.stream().filter(project -> projectIds.contains(project.getId())).toList();
        }
        @Override public Optional<Project> findById(Long projectId) {
            return projects.stream().filter(project -> projectId.equals(project.getId())).findFirst();
        }
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
        private final List<ProjectMember> members = new ArrayList<>();

        @Override public List<ProjectMember> selectByProjectId(Long projectId) {
            return members.stream().filter(member -> projectId.equals(member.getProjectId())).toList();
        }
        @Override public ProjectMember selectByProjectIdAndUserId(Long projectId, Long userId) {
            return members.stream()
                    .filter(member -> projectId.equals(member.getProjectId()) && userId.equals(member.getUserId()))
                    .findFirst()
                    .orElse(null);
        }
        @Override public int countActiveMember(Long projectId, Long userId) {
            ProjectMember member = selectByProjectIdAndUserId(projectId, userId);
            return member != null && "ENABLED".equals(member.getStatus()) ? 1 : 0;
        }
        @Override public int insert(ProjectMember member) { members.add(member); return 1; }
        @Override public int update(ProjectMember member) { return 1; }
        @Override public int deleteByProjectIdAndUserId(Long projectId, Long userId, Long operatorId) { return 1; }
        @Override public List<Long> selectProjectIdsByUserId(Long userId) {
            return members.stream()
                    .filter(member -> userId.equals(member.getUserId()))
                    .map(ProjectMember::getProjectId)
                    .toList();
        }
            @Override public List<ProjectMember> selectEnabledByUserId(Long userId) { return List.of(); }
}
}
