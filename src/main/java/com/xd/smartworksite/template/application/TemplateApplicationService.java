package com.xd.smartworksite.template.application;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateUpdateRequest;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateApplicationService {

    private static final String FILE_STATUS_ACTIVE = "ACTIVE";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*([A-Za-z0-9_.-]+)\\s*}");

    private final TemplateRepository templateRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final StorageAdapter storageAdapter;

    public TemplateApplicationService(TemplateRepository templateRepository,
                                      ProjectAccessApplicationService projectAccessApplicationService,
                                      StorageAdapter storageAdapter) {
        this.templateRepository = templateRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.storageAdapter = storageAdapter;
    }

    @Transactional
    public TemplateResponse uploadTemplate(Long projectId,
                                           String templateCategory,
                                           String templateName,
                                           String templateType,
                                           String scenario,
                                           String versionNo,
                                           String description,
                                           MultipartFile file) {
        projectAccessApplicationService.requireProjectWritableAccess(projectId);
        TemplateCategory category = parseCategory(templateCategory);
        requireText(templateName, "模板名称不能为空");
        requireText(templateType, "模板类型不能为空");
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件不能为空");
        }

        String normalizedVersion = normalizeVersion(versionNo);
        String originalFilename = normalizeFileName(file.getOriginalFilename());
        String objectName = buildObjectName(projectId, category.name(), originalFilename);
        StorageObject storageObject;
        try {
            storageObject = storageAdapter.upload(objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取模板文件失败");
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传模板文件失败");
        }

        FileObjectRecord fileObject = new FileObjectRecord();
        fileObject.setProjectId(projectId);
        fileObject.setBizType(category == TemplateCategory.REPORT ? "REPORT_TEMPLATE" : "REVIEW_TEMPLATE");
        fileObject.setFileName(originalFilename);
        fileObject.setObjectName(storageObject.getObjectName());
        fileObject.setContentType(storageObject.getContentType());
        fileObject.setFileSize(storageObject.getSize());
        fileObject.setStatus(FILE_STATUS_ACTIVE);
        fileObject.setMetadata("{}");
        templateRepository.saveFileObject(fileObject);
        if (fileObject.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "template file id was not generated");
        }

        Template template = new Template();
        template.setProjectId(projectId);
        template.setTemplateName(templateName.trim());
        template.setTemplateCategory(category.name());
        template.setTemplateType(templateType.trim());
        template.setScenario(trimToNull(scenario));
        template.setVersionNo(normalizedVersion);
        template.setFileId(fileObject.getId());
        template.setStatus(TemplateStatus.ENABLED.name());
        template.setDescription(trimToNull(description));
        template.setMetadata("{}");
        templateRepository.save(template);
        if (template.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "template id was not generated");
        }
        requireUpdated(templateRepository.updateFileBizId(fileObject.getId(), template.getId()), "template file business id update failed");

        return toResponse(templateRepository.findById(template.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "template record is not readable")));
    }

    public PageResult<TemplateResponse> queryTemplates(TemplateQueryRequest request) {
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        }
        List<Long> accessibleProjectIds = resolveAccessibleProjectIds(request.getProjectId());
        if (request.getProjectId() == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return new PageResult<>(request.getPageNo(), request.getPageSize(), 0, List.of());
        }
        String category = normalizeOptionalCategory(request.getTemplateCategory());
        String status = normalizeOptionalStatus(request.getStatus());
        Page<Template> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> templateRepository.findPage(
                        request.getProjectId(),
                        accessibleProjectIds,
                        category,
                        trimToNull(request.getTemplateType()),
                        status,
                        trimToNull(request.getKeyword())
                ));
        List<TemplateResponse> records = page.getResult().stream().map(this::toResponse).toList();
        return new PageResult<>(request.getPageNo(), request.getPageSize(), page.getTotal(), records);
    }

    public List<TemplateResponse> listTemplates(TemplateQueryRequest request) {
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        }
        List<Long> accessibleProjectIds = resolveAccessibleProjectIds(request.getProjectId());
        if (request.getProjectId() == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return List.of();
        }
        String category = normalizeOptionalCategory(request.getTemplateCategory());
        String status = normalizeOptionalStatus(request.getStatus());
        return templateRepository.findPage(
                        request.getProjectId(),
                        accessibleProjectIds,
                        category,
                        trimToNull(request.getTemplateType()),
                        status,
                        trimToNull(request.getKeyword())
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TemplateResponse getTemplate(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectAccess(template.getProjectId());
        return toResponse(template);
    }

    public List<String> listTemplateVariables(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectAccess(template.getProjectId());
        if (!TemplateCategory.REPORT.name().equals(template.getTemplateCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "template is not a report template");
        }
        FileObjectRecord fileObject = templateRepository.findFileObjectById(template.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "template file not found"));
        if (!template.getProjectId().equals(fileObject.getProjectId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "template file project mismatch");
        }
        String text = readTemplateText(fileObject);
        return extractVariables(text);
    }

    @Transactional
    public TemplateResponse updateTemplate(Long templateId, TemplateUpdateRequest request) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectWritableManage(template.getProjectId());
        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateType(request.getTemplateType().trim());
        template.setScenario(trimToNull(request.getScenario()));
        template.setVersionNo(normalizeVersion(request.getVersionNo()));
        template.setDescription(trimToNull(request.getDescription()));
        int updated = templateRepository.update(template);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "template update failed");
        }
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public TemplateResponse enableTemplate(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectWritableManage(template.getProjectId());
        int updated = templateRepository.updateStatus(templateId, TemplateStatus.ENABLED.name());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "template enable failed");
        }
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public TemplateResponse disableTemplate(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectWritableManage(template.getProjectId());
        int updated = templateRepository.updateStatus(templateId, TemplateStatus.DISABLED.name());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "template disable failed");
        }
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectWritableManage(template.getProjectId());
        int updated = templateRepository.delete(templateId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "template delete failed");
        }
    }

    private Template requireTemplate(Long templateId) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
    }

    private List<Long> resolveAccessibleProjectIds(Long projectId) {
        if (projectId != null || SecurityUtils.isPlatformAdmin()) {
            return null;
        }
        return projectAccessApplicationService.currentUserAccessibleProjectIds();
    }

    private TemplateCategory parseCategory(String templateCategory) {
        try {
            return TemplateCategory.parse(templateCategory);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板分类必须是 REVIEW 或 REPORT");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String normalizeVersion(String versionNo) {
        if (versionNo == null || versionNo.isBlank()) {
            return "v1";
        }
        return versionNo.trim();
    }

    private String normalizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件名不能为空");
        }
        return filename.replace("\\", "/").substring(filename.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }

    private String buildObjectName(Long projectId, String category, String filename) {
        String suffix = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            suffix = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        return "templates/project-" + projectId + "/" + category.toLowerCase(Locale.ROOT) + "/"
                + LocalDate.now() + "/" + UUID.randomUUID() + suffix;
    }

    private String normalizeOptionalCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TemplateCategory.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "templateCategory must be REVIEW or REPORT");
        }
    }

    private String normalizeOptionalStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TemplateStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String readTemplateText(FileObjectRecord fileObject) {
        String filename = fileObject.getFileName() == null ? "" : fileObject.getFileName().toLowerCase(Locale.ROOT);
        try (InputStream inputStream = storageAdapter.openObject(fileObject.getObjectName())) {
            if (filename.endsWith(".docx")) {
                return readDocxText(inputStream);
            }
            if (filename.endsWith(".txt") || filename.endsWith(".md")) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "report template variable parsing supports DOCX, TXT or MD files");
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取报告模板变量失败");
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报告模板文件读取失败");
        }
    }

    private String readDocxText(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                builder.append(paragraph.getText()).append('\n');
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        builder.append(cell.getText()).append('\n');
                    }
                }
            }
        }
        return builder.toString();
    }

    private List<String> extractVariables(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.CONFLICT, "report template content is empty");
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return new ArrayList<>(variables);
    }

    private TemplateResponse toResponse(Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setTemplateId(template.getId());
        response.setProjectId(template.getProjectId());
        response.setTaskId(0L);
        response.setFileId(template.getFileId());
        response.setTemplateCategory(template.getTemplateCategory());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateType(template.getTemplateType());
        response.setScenario(template.getScenario());
        response.setVersionNo(template.getVersionNo());
        response.setStatus(template.getStatus());
        response.setDescription(template.getDescription());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
