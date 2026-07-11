package com.xd.smartworksite.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.report.dto.ReportCreateRequest;
import com.xd.smartworksite.report.dto.ReportCreateResponse;
import com.xd.smartworksite.report.dto.ReportQueryRequest;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateRequest;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateResponse;
import com.xd.smartworksite.report.infra.CryptoAgentV3ReportClient;
import com.xd.smartworksite.report.infra.CryptoAgentV3Properties;
import com.xd.smartworksite.report.infra.GeneratedFilePayload;
import com.xd.smartworksite.report.repository.ReportRepository;
import com.xd.smartworksite.task.application.TaskOutboxApplicationService;
import com.xd.smartworksite.task.domain.TaskOutboxEvent;
import com.xd.smartworksite.task.domain.TaskStageLog;
import com.xd.smartworksite.task.domain.TaskStatusCount;
import com.xd.smartworksite.task.repository.TaskRepository;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportGenerationApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "admin", List.of("PLATFORM_ADMIN"), List.of(), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReportCreatesQueuedTaskWithoutCallingCryptoAgent() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateRequest request = createRequest();

        ReportCreateResponse response = service.createReport(request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTaskId()).isEqualTo(1L);
        assertThat(cryptoClient.lastRequest).isNull();
        assertThat(reportRepository.tasks).hasSize(1);
        assertThat(reportRepository.tasks.get(0).getStatus()).isEqualTo("QUEUED");
        assertThat(reportRepository.outboxService.enqueuedTaskIds).containsExactly(1L);
        assertThat(reportRepository.fileObjects).isEmpty();
        assertThat(reportRepository.versions).isEmpty();
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("PENDING");    }

    @Test
    void createReportRejectsBlankReportName() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateRequest request = createRequest();
        request.setReportName(" ");

        assertThatThrownBy(() -> service.createReport(request))
                .hasMessageContaining("报告名称不能为空");
        assertThat(reportRepository.reports).isEmpty();
        assertThat(reportRepository.tasks).isEmpty();
    }

    @Test
    void regenerateReportKeepsOriginalGenerationParams() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse first = service.createReport(createRequest());

        ReportCreateResponse regenerated = service.regenerateReport(first.getReportId());

        assertThat(reportRepository.configs).hasSize(2);
        assertThat(regenerated.getStatus()).isEqualTo("PENDING");
        assertThat(cryptoClient.lastRequest).isNull();
        assertThat(reportRepository.outboxService.enqueuedTaskIds).containsExactly(1L, 2L);
    }


    @Test
    void downloadReportSavesWordOnDemandAndReturnsAccessUrl() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());
        service.executeReportTask(created.getReportId(), created.getTaskId());

        String downloadUrl = service.createDownloadUrl(created.getReportId(), "WORD");

        assertThat(downloadUrl).startsWith("http://127.0.0.1/reports/project-1/report-1/");
        assertThat(reportRepository.fileObjects).hasSize(1);
        assertThat(reportRepository.versions.get(0).getWordFileId()).isEqualTo(1L);
    }

    @Test
    void executeReportTaskFailsWhenCryptoAgentReturnsBlankFileName() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        cryptoClient.generatedFileName = " ";
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .hasMessageContaining("CryptoAgentV3返回文件名不能为空");
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("FAILED");
        assertThat(reportRepository.reports.get(0).getErrorMessage()).contains("CryptoAgentV3返回文件名不能为空");
        assertThat(reportRepository.fileObjects).isEmpty();
        assertThat(reportRepository.versions).isEmpty();
    }


    @Test
    void executeReportTaskRejectsReferenceFileFromAnotherProject() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateRequest request = createRequest();
        request.setVariables(Map.of());
        request.setReferenceFileIds(List.of(99L));
        FileObjectRecord foreignFile = new FileObjectRecord();
        foreignFile.setId(99L);
        foreignFile.setProjectId(2L);
        foreignFile.setFileName("foreign.txt");
        foreignFile.setContentType("text/plain");
        foreignFile.setObjectName("foreign.txt");
        reportRepository.fileObjects.add(foreignFile);
        ReportCreateResponse created = service.createReport(request);

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .hasMessageContaining("99");
        assertThat(cryptoClient.lastRequest).isNull();
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void executeReportTaskRejectsDisabledProjectBeforeCallingCryptoAgent() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        MutableProjectRepository projectRepository = new MutableProjectRepository();
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient, projectRepository);
        ReportCreateResponse created = service.createReport(createRequest());
        projectRepository.status = "DISABLED";

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("project is not enabled");

        assertThat(cryptoClient.lastRequest).isNull();
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void executeReportTaskFailsFastWhenProcessingStateCannotBePersisted() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        reportRepository.failReportProcessingUpdate = true;
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("report processing status update failed");

        assertThat(cryptoClient.lastRequest).isNull();
    }

    @Test
    void executeReportTaskFailsFastWhenSuccessStateCannotBePersisted() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        reportRepository.failReportSuccessUpdate = true;
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("report success status update failed");

        assertThat(cryptoClient.lastRequest).isNotNull();
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void executeReportTaskFailsFastWhenFailedStateCannotBePersisted() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        reportRepository.failReportFailedUpdate = true;
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        cryptoClient.generatedFileName = " ";
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());

        assertThatThrownBy(() -> service.executeReportTask(created.getReportId(), created.getTaskId()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT.getCode()))
                .hasMessageContaining("report failed status update failed");
    }

    @Test
    void queryReportsRejectsInvalidStatus() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportQueryRequest request = new ReportQueryRequest();
        request.setProjectId(1L);
        request.setStatus("UNKNOWN");

        assertThatThrownBy(() -> service.queryReports(request))
                .hasMessageContaining("status must be");
    }

    @Test
    void platformAdminQueryReportsWithoutProjectDoesNotApplyMemberProjectFilter() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        service.createReport(createRequest());

        ReportQueryRequest request = new ReportQueryRequest();
        service.queryReports(request);

        assertThat(reportRepository.lastAccessibleProjectIds).isNull();
    }

    private ReportGenerationApplicationService newService(InMemoryReportRepository reportRepository,
                                                          CapturingCryptoAgentClient cryptoClient) {
        return newService(reportRepository, cryptoClient, new MutableProjectRepository());
    }

    private ReportGenerationApplicationService newService(InMemoryReportRepository reportRepository,
                                                          CapturingCryptoAgentClient cryptoClient,
                                                          ProjectRepository projectRepository) {
        return new ReportGenerationApplicationService(
                reportRepository,
                new ProjectAccessApplicationService(projectRepository, new EmptyProjectMemberMapper()),
                templateRepository(),
                reportRepository.outboxService,
                new MemoryStorageAdapter(),
                cryptoClient,
                objectMapper
        );
    }

    private static class EmptyProjectMemberMapper implements ProjectMemberMapper {
        @Override
        public List<ProjectMember> selectByProjectId(Long projectId) {
            return List.of();
        }

        @Override
        public ProjectMember selectByProjectIdAndUserId(Long projectId, Long userId) {
            return null;
        }

        @Override
        public int countActiveMember(Long projectId, Long userId) {
            return 0;
        }

        @Override
        public int insert(ProjectMember member) {
            return 1;
        }

        @Override
        public int update(ProjectMember member) {
            return 1;
        }

        @Override
        public int deleteByProjectIdAndUserId(Long projectId, Long userId, Long operatorId) {
            return 1;
        }

        @Override
        public List<Long> selectProjectIdsByUserId(Long userId) {
            return List.of();
        }
            @Override public List<ProjectMember> selectEnabledByUserId(Long userId) { return List.of(); }
}

    private ReportCreateRequest createRequest() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileId", "manual-1");
        document.put("fileName", "密测过程信息.txt");
        document.put("content", "这里是密测过程信息。");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("referenceDocuments", List.of(document));

        ReportCreateRequest request = new ReportCreateRequest();
        request.setProjectId(1L);
        request.setReportName("密评报告测试");
        request.setReportType("CRYPTO_EVALUATION_REPORT");
        request.setTemplateId(1001L);
        request.setVariables(variables);
        return request;
    }

    private static class MutableProjectRepository implements ProjectRepository {
            private String status = "ENABLED";

            @Override
            public List<Project> findPage(String keyword, String status) {
                return List.of();
            }

            @Override
            public List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds) {
                return List.of();
            }

            @Override
            public Optional<Project> findById(Long projectId) {
                Project project = new Project();
                project.setId(projectId);
                project.setStatus(status);
                return Optional.of(project);
            }

            @Override
            public Optional<Project> findByProjectCode(String projectCode) {
                return Optional.empty();
            }

            @Override
            public Project insert(Project project) {
                return project;
            }

            @Override
            public int update(Project project) {
                return 1;
            }

            @Override
            public int softDelete(Long projectId, Long updatedBy) {
                return 1;
            }

            @Override
            public int updateStatus(Long projectId, String status, Long updatedBy) {
                return 1;
            }

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

    private TemplateRepository templateRepository() {
        return new TemplateRepository() {
            @Override
            public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
                return fileObject;
            }

            @Override
            public Optional<FileObjectRecord> findFileObjectById(Long fileId) {
                return Optional.empty();
            }

            @Override
            public int updateFileBizId(Long fileId, Long bizId) {
                return 1;
            }

            @Override
            public Template save(Template template) {
                return template;
            }

            @Override
            public Optional<Template> findById(Long templateId) {
                Template template = new Template();
                template.setId(templateId);
                template.setProjectId(1L);
                template.setTemplateCategory(TemplateCategory.REPORT.name());
                template.setStatus(TemplateStatus.ENABLED.name());
                return Optional.of(template);
            }

            @Override
            public List<Template> findPage(Long projectId, List<Long> accessibleProjectIds, String templateCategory, String templateType, String status, String keyword) {
                return List.of();
            }

            @Override
            public int update(Template template) {
                return 1;
            }

            @Override
            public int updateStatus(Long templateId, String status) {
                return 1;
            }

            @Override
            public int delete(Long templateId) {
                return 1;
            }
        };
    }

    private String generatedFileUrl() throws Exception {
        byte[] docxBytes = "generated-docx".getBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report.docx", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            exchange.sendResponseHeaders(200, docxBytes.length);
            exchange.getResponseBody().write(docxBytes);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/report.docx";
    }

    private static class CapturingCryptoAgentClient extends CryptoAgentV3ReportClient {
        private final String generatedFileUrl;
        private String generatedFileName = "密评报告.docx";
        private CryptoAgentGenerateRequest lastRequest;

        CapturingCryptoAgentClient(ObjectMapper objectMapper, String generatedFileUrl) {
            super(new CryptoAgentV3Properties(), objectMapper);
            this.generatedFileUrl = generatedFileUrl;
        }

        @Override
        public CryptoAgentGenerateResponse generate(CryptoAgentGenerateRequest requestPayload) {
            this.lastRequest = requestPayload;
            GeneratedFilePayload file = new GeneratedFilePayload();
            file.setFileType("DOCX");
            file.setFileName(generatedFileName);
            file.setDownloadRef(generatedFileUrl);
            CryptoAgentGenerateResponse response = new CryptoAgentGenerateResponse();
            response.setSuccess(true);
            response.setTaskId(requestPayload.getTaskId());
            response.setReportId(requestPayload.getReportId());
            response.setGeneratedFiles(List.of(file));
            return response;
        }
    }

    private static class MemoryStorageAdapter implements StorageAdapter {
        @Override
        public StorageObject upload(String objectName, InputStream inputStream, long size, String contentType) {
            return new StorageObject(objectName, "test-bucket",contentType, size);
        }

        
        @Override
        public InputStream openObject(String objectName) {
            return InputStream.nullInputStream();
        }

        @Override
        public String createAccessUrl(String objectName, Duration expire) {
            return "http://127.0.0.1/" + objectName;
        }

        @Override
        public void delete(String objectName) {
        }
    }

    private class RecordingTaskOutboxService extends TaskOutboxApplicationService {
        private final List<Long> enqueuedTaskIds = new ArrayList<>();

        RecordingTaskOutboxService() {
            super(new NoopTaskRepository(), null, null);
        }

        @Override
        public void enqueueTask(com.xd.smartworksite.task.domain.GenerateTask task, String reason) {
            enqueuedTaskIds.add(task.getId());
        }
    }

    private static class NoopTaskRepository implements TaskRepository {
        @Override public com.xd.smartworksite.task.domain.GenerateTask insertTask(com.xd.smartworksite.task.domain.GenerateTask task) { return task; }
        @Override public Optional<com.xd.smartworksite.task.domain.GenerateTask> findById(Long taskId) { return Optional.empty(); }
        @Override public List<com.xd.smartworksite.task.domain.GenerateTask> findPage(Long projectId, List<Long> accessibleProjectIds, String taskType, String status, java.time.LocalDateTime createdFrom, java.time.LocalDateTime createdTo) { return List.of(); }
        @Override public List<TaskStageLog> findStages(Long taskId) { return List.of(); }
        @Override public List<TaskStatusCount> countByStatus(Long projectId, List<Long> accessibleProjectIds) { return List.of(); }
        @Override public int markRetrying(Long taskId, String nextStatus, String currentStage, Long updatedBy) { return 0; }
        @Override public int cancelWaiting(Long taskId, Long updatedBy) { return 0; }
        @Override public int requestRunningCancel(Long taskId, Long updatedBy) { return 0; }
        @Override public int claimQueuedTask(Long taskId, String workerId, long leaseSeconds, String currentStage) { return 0; }
        @Override public int heartbeat(Long taskId, String workerId, long leaseSeconds) { return 0; }
        @Override public int completeSuccess(Long taskId, String workerId, String currentStage) { return 0; }
        @Override public int completeFailure(Long taskId, String workerId, String currentStage, String errorMessage) { return 0; }
        @Override public int completeCanceled(Long taskId, String workerId, String currentStage, String errorMessage) { return 0; }
        @Override public int insertStage(TaskStageLog log) { return 1; }
        @Override public int insertOutboxEvent(TaskOutboxEvent event) { return 1; }
        @Override public Optional<TaskOutboxEvent> findOutboxEvent(Long taskId, String eventType) { return Optional.empty(); }
        @Override public List<TaskOutboxEvent> findDueOutboxEvents(int limit) { return List.of(); }
        @Override public int markOutboxDelivered(Long eventId) { return 0; }
        @Override public int markOutboxFailed(Long eventId, String status, String errorMessage, long nextDeliverySeconds) { return 0; }
    }
    private class InMemoryReportRepository implements ReportRepository {
        private long nextConfigId = 1L;
        private long nextReportId = 1L;
        private long nextTaskId = 1L;
        private long nextFileId = 1L;
        private long nextVersionId = 1L;
        private final List<ReportConfig> configs = new ArrayList<>();
        private final List<Report> reports = new ArrayList<>();
        private final List<GenerateTask> tasks = new ArrayList<>();
        private final List<FileObjectRecord> fileObjects = new ArrayList<>();
        private final List<ReportVersion> versions = new ArrayList<>();
        private final RecordingTaskOutboxService outboxService = new RecordingTaskOutboxService();
        private List<Long> lastAccessibleProjectIds;
        private boolean failReportProcessingUpdate;
        private boolean failReportSuccessUpdate;
        private boolean failReportFailedUpdate;

        @Override
        public ReportConfig saveConfig(ReportConfig config) {
            config.setId(nextConfigId++);
            configs.add(config);
            return config;
        }

        @Override
        public Report saveReport(Report report) {
            report.setId(nextReportId++);
            reports.add(report);
            return report;
        }

        @Override
        public GenerateTask saveTask(GenerateTask task) {
            task.setId(nextTaskId++);
            tasks.add(task);
            return task;
        }

        @Override
        public int updateReportTask(Long reportId, Long taskId) {
            findReport(reportId).setTaskId(taskId);
            return 1;
        }

        @Override
        public int updateTaskBizId(Long taskId, Long bizId) {
            return tasks.stream().anyMatch(task -> taskId.equals(task.getId())) ? 1 : 0;
        }

        @Override
        public int updateReportProcessing(Long reportId, String status, int progress, String currentStage) {
            if (failReportProcessingUpdate) {
                return 0;
            }
            Report report = findReport(reportId);
            report.setStatus(status);
            report.setProgress(progress);
            return 1;
        }

        @Override
        public int updateReportSuccess(Long reportId, Long versionId, String status, int progress, String previewUrl) {
            if (failReportSuccessUpdate) {
                return 0;
            }
            Report report = findReport(reportId);
            report.setCurrentVersionId(versionId);
            report.setStatus(status);
            report.setProgress(progress);
            report.setPreviewUrl(previewUrl);
            return 1;
        }

        @Override
        public int updateReportFailed(Long reportId, String status, String errorMessage) {
            if (failReportFailedUpdate) {
                return 0;
            }
            Report report = findReport(reportId);
            report.setStatus(status);
            report.setErrorMessage(errorMessage);
            return 1;
        }

        @Override
        public int updateTaskStatus(Long taskId, String status, String currentStage, String errorMessage) {
            return tasks.stream()
                    .filter(task -> taskId.equals(task.getId()))
                    .findFirst()
                    .map(task -> {
                        task.setStatus(status);
                        task.setCurrentStage(currentStage);
                        return 1;
                    })
                    .orElse(0);
        }

        @Override
        public int updateVersionWordFile(Long versionId, Long wordFileId, String contentHash) {
            return versions.stream()
                    .filter(version -> versionId.equals(version.getId()))
                    .findFirst()
                    .map(version -> {
                        version.setWordFileId(wordFileId);
                        return 1;
                    })
                    .orElse(0);
        }

        @Override
        public Optional<FileObjectRecord> findFileObjectById(Long fileId) {
            return fileObjects.stream().filter(file -> fileId.equals(file.getId())).findFirst();
        }

        @Override
        public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
            fileObject.setId(nextFileId++);
            fileObjects.add(fileObject);
            return fileObject;
        }

        @Override
        public ReportVersion saveVersion(ReportVersion version) {
            version.setId(nextVersionId++);
            versions.add(version);
            return version;
        }

        @Override
        public Optional<ReportConfig> findConfigById(Long configId) {
            return configs.stream().filter(config -> configId.equals(config.getId())).findFirst();
        }

        @Override
        public Optional<Long> findCurrentWordFileId(Long reportId) {
            return versions.stream()
                    .filter(version -> reportId.equals(version.getReportId()))
                    .findFirst()
                    .map(ReportVersion::getWordFileId);
        }

        @Override
        public Optional<Report> findReportById(Long reportId) {
            return reports.stream().filter(report -> reportId.equals(report.getId())).findFirst();
        }

        @Override
        public List<Report> findReportPage(Long projectId, List<Long> accessibleProjectIds, String reportType, String status, String keyword) {
            this.lastAccessibleProjectIds = accessibleProjectIds;
            return reports;
        }

        private Report findReport(Long reportId) {
            return findReportById(reportId).orElseThrow();
        }
    }
}
