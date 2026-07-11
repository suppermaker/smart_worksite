package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.datasource.domain.DataSource;
import com.xd.smartworksite.datasource.dto.DataSourceConnectionTestResponse;
import com.xd.smartworksite.datasource.dto.DataSourceCreateRequest;
import com.xd.smartworksite.datasource.dto.DataSourceQueryRequest;
import com.xd.smartworksite.datasource.dto.DataSourceSchemaResponse;
import com.xd.smartworksite.datasource.dto.DataSourceUpdateRequest;
import com.xd.smartworksite.datasource.repository.DataSourceRepository;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.project.domain.Project;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSourceApplicationServiceTest {
    private InMemoryDataSourceRepository dataSourceRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryProjectMemberMapper memberMapper;
    private DataSourceApplicationService service;
    private StubJdbcDataSourceInspector jdbcDataSourceInspector;

    @BeforeEach
    void setUp() {
        setCurrentUser(2L, List.of("PROJECT_USER"));
        dataSourceRepository = new InMemoryDataSourceRepository();
        projectRepository = new InMemoryProjectRepository();
        memberMapper = new InMemoryProjectMemberMapper();
        projectRepository.insert(project(1L));
        projectRepository.insert(project(2L));
        memberMapper.insert(member(1L, 2L, "PROJECT_ADMIN", "ENABLED"));
        DataSourcePasswordCipher passwordCipher = mock(DataSourcePasswordCipher.class);
        when(passwordCipher.encrypt(anyString())).thenAnswer(invocation -> "AES_GCM:test-cipher-" + invocation.getArgument(0));
        jdbcDataSourceInspector = new StubJdbcDataSourceInspector(passwordCipher);
        service = new DataSourceApplicationService(
                dataSourceRepository,
                new ProjectAccessApplicationService(projectRepository, memberMapper),
                passwordCipher,
                jdbcDataSourceInspector
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDataSourceEncryptsPasswordAndDoesNotExposeCipher() {
        var response = service.createDataSource(createRequest(1L, "mysql-main"));

        assertThat(response.getDataSourceId()).isEqualTo(1L);
        assertThat(response.getDbType()).isEqualTo("MYSQL");
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("createdAt", "updatedAt");
        assertThat(dataSourceRepository.findById(1L).orElseThrow().getPasswordCipher()).startsWith("AES_GCM:");
    }

    @Test
    void updateKeepsPasswordWhenBlankAndCanDisableEnable() {
        var created = service.createDataSource(createRequest(1L, "mysql-main"));
        String oldCipher = dataSourceRepository.findById(created.getDataSourceId()).orElseThrow().getPasswordCipher();
        DataSourceUpdateRequest update = updateRequest("pg-main", "");

        var updated = service.updateDataSource(created.getDataSourceId(), update);
        var disabled = service.disableDataSource(created.getDataSourceId());
        var enabled = service.enableDataSource(created.getDataSourceId());

        assertThat(updated.getDbType()).isEqualTo("POSTGRESQL");
        assertThat(dataSourceRepository.findById(created.getDataSourceId()).orElseThrow().getPasswordCipher()).isEqualTo(oldCipher);
        assertThat(disabled.getStatus()).isEqualTo("DISABLED");
        assertThat(enabled.getStatus()).isEqualTo("ENABLED");
    }

    @Test
    void updateFailsFastWhenRepositoryDoesNotUpdateRow() {
        var created = service.createDataSource(createRequest(1L, "mysql-main"));
        dataSourceRepository.failUpdate = true;

        assertThatThrownBy(() -> service.updateDataSource(created.getDataSourceId(), updateRequest("pg-main", "")))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    @Test
    void queryWithoutProjectIsLimitedToAccessibleProjects() {
        service.createDataSource(createRequest(1L, "project-one"));
        dataSourceRepository.insert(dataSource(2L, "project-two"));

        service.queryDataSources(new DataSourceQueryRequest());
        var records = dataSourceRepository.findPage(null, List.of(1L), null, null, null);

        assertThat(records).extracting(DataSource::getName).containsExactly("project-one");
    }

    @Test
    void nonMemberCannotReadOtherProjectDataSource() {
        DataSource foreign = dataSourceRepository.insert(dataSource(2L, "project-two"));

        assertThatThrownBy(() -> service.getDataSource(foreign.getId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void deleteRemovesDataSourceLogically() {
        var created = service.createDataSource(createRequest(1L, "mysql-main"));

        service.deleteDataSource(created.getDataSourceId());

        assertThat(dataSourceRepository.findById(created.getDataSourceId())).isEmpty();
    }

    @Test
    void testConnectionUsesRealInspectorResultAndProjectAccess() {
        var created = service.createDataSource(createRequest(1L, "mysql-main"));

        var response = service.testConnection(created.getDataSourceId());

        assertThat(response.isSuccess()).isTrue();
        assertThat(jdbcDataSourceInspector.lastConnectionTestDataSource.getId()).isEqualTo(created.getDataSourceId());
    }

    @Test
    void inspectSchemaRejectsDisabledDataSource() {
        var created = service.createDataSource(createRequest(1L, "mysql-main"));
        service.disableDataSource(created.getDataSourceId());

        assertThatThrownBy(() -> service.inspectSchema(created.getDataSourceId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()));
    }

    private DataSourceCreateRequest createRequest(Long projectId, String name) {
        DataSourceCreateRequest request = new DataSourceCreateRequest();
        request.setProjectId(projectId);
        request.setName(name);
        request.setDbType("mysql");
        request.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/test");
        request.setUsername("readonly");
        request.setPassword("secret");
        return request;
    }

    private DataSourceUpdateRequest updateRequest(String name, String password) {
        DataSourceUpdateRequest request = new DataSourceUpdateRequest();
        request.setName(name);
        request.setDbType("postgresql");
        request.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/test");
        request.setUsername("readonly");
        request.setPassword(password);
        return request;
    }

    private DataSource dataSource(Long projectId, String name) {
        DataSource dataSource = new DataSource();
        dataSource.setProjectId(projectId);
        dataSource.setName(name);
        dataSource.setDbType("MYSQL");
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/test");
        dataSource.setUsername("readonly");
        dataSource.setPasswordCipher("AES_GCM:test");
        dataSource.setStatus("ENABLED");
        return dataSource;
    }

    private Project project(Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setProjectName("项目" + projectId);
        project.setProjectCode("SITE-" + projectId);
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

    private static class StubJdbcDataSourceInspector extends JdbcDataSourceInspector {
        private DataSource lastConnectionTestDataSource;

        StubJdbcDataSourceInspector(DataSourcePasswordCipher passwordCipher) {
            super(passwordCipher, 1, 10, 10);
        }

        @Override
        public DataSourceConnectionTestResponse testConnection(DataSource dataSource) {
            lastConnectionTestDataSource = dataSource;
            DataSourceConnectionTestResponse response = new DataSourceConnectionTestResponse();
            response.setDataSourceId(dataSource.getId());
            response.setDbType(dataSource.getDbType());
            response.setSuccess(true);
            response.setElapsedMs(1L);
            return response;
        }

        @Override
        public DataSourceSchemaResponse inspectSchema(DataSource dataSource) {
            DataSourceSchemaResponse response = new DataSourceSchemaResponse();
            response.setDataSourceId(dataSource.getId());
            response.setDbType(dataSource.getDbType());
            return response;
        }
    }

    private static class InMemoryDataSourceRepository implements DataSourceRepository {
        private long nextId = 1L;
        private final List<DataSource> records = new ArrayList<>();
        private boolean failUpdate;

        @Override
        public DataSource insert(DataSource dataSource) {
            dataSource.setId(nextId++);
            dataSource.setCreatedAt(LocalDateTime.now());
            dataSource.setUpdatedAt(dataSource.getCreatedAt());
            records.add(dataSource);
            return dataSource;
        }

        @Override
        public Optional<DataSource> findById(Long dataSourceId) {
            return records.stream().filter(record -> dataSourceId.equals(record.getId())).findFirst();
        }

        @Override
        public List<DataSource> findPage(Long projectId, List<Long> accessibleProjectIds, String dbType, String status, String keyword) {
            return records.stream()
                    .filter(record -> projectId == null || projectId.equals(record.getProjectId()))
                    .filter(record -> accessibleProjectIds == null || accessibleProjectIds.contains(record.getProjectId()))
                    .filter(record -> dbType == null || dbType.equals(record.getDbType()))
                    .filter(record -> status == null || status.equals(record.getStatus()))
                    .filter(record -> keyword == null || record.getName().contains(keyword))
                    .toList();
        }

        @Override
        public int update(DataSource dataSource) {
            if (failUpdate) {
                return 0;
            }
            DataSource current = findById(dataSource.getId()).orElseThrow();
            current.setName(dataSource.getName());
            current.setDbType(dataSource.getDbType());
            current.setJdbcUrl(dataSource.getJdbcUrl());
            current.setUsername(dataSource.getUsername());
            current.setPasswordCipher(dataSource.getPasswordCipher());
            current.setUpdatedBy(dataSource.getUpdatedBy());
            return 1;
        }

        @Override
        public int updateStatus(Long dataSourceId, String status, Long updatedBy) {
            return findById(dataSourceId).map(record -> {
                record.setStatus(status);
                record.setUpdatedBy(updatedBy);
                return 1;
            }).orElse(0);
        }

        @Override
        public int softDelete(Long dataSourceId, Long updatedBy) {
            return records.removeIf(record -> dataSourceId.equals(record.getId())) ? 1 : 0;
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
