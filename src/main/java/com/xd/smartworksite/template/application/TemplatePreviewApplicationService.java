package com.xd.smartworksite.template.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.file.application.FileObjectApplicationService;
import com.xd.smartworksite.file.application.FileObjectContent;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.dto.TemplatePreviewFile;
import com.xd.smartworksite.template.infra.TemplateFileSupport;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TemplatePreviewApplicationService {

    private final TemplateRepository templateRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final FileObjectApplicationService fileObjectApplicationService;

    public TemplatePreviewApplicationService(TemplateRepository templateRepository,
                                             ProjectAccessApplicationService projectAccessApplicationService,
                                             FileObjectApplicationService fileObjectApplicationService) {
        this.templateRepository = templateRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.fileObjectApplicationService = fileObjectApplicationService;
    }

    public TemplatePreviewFile openPreview(Long templateId) {
        Template template = requireTemplate(templateId);
        projectAccessApplicationService.requireProjectAccess(template.getProjectId());
        FileObjectContent file = fileObjectApplicationService.openFileContent(
                template.getFileId(), template.getProjectId(), template.getId());
        if (!TemplateFileSupport.isSupported(file.getFileName())) {
            closeQuietly(file);
            String format = TemplateFileSupport.isPdf(file.getFileName()) ? "PDF" : TemplateFileSupport.extension(file.getFileName());
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported template preview format: " + format);
        }
        return new TemplatePreviewFile(
                file.getFileName(),
                TemplateFileSupport.resolveContentType(file.getFileName(), file.getContentType()),
                file.getFileSize(),
                file.getInputStream()
        );
    }

    private Template requireTemplate(Long templateId) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
    }

    private void closeQuietly(FileObjectContent file) {
        try {
            file.getInputStream().close();
        } catch (IOException ignored) {
        }
    }
}
