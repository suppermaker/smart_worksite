package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.mapper.TemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTemplateRepository implements TemplateRepository {

    private final TemplateMapper templateMapper;

    public MyBatisTemplateRepository(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
        int inserted = templateMapper.insertFileObject(fileObject);
        if (inserted <= 0 || fileObject.getId() == null) {
            throw new IllegalStateException("template file object insert failed or id was not generated");
        }
        return fileObject;
    }

    @Override
    public Optional<FileObjectRecord> findFileObjectById(Long fileId) {
        return Optional.ofNullable(templateMapper.selectFileObjectById(fileId));
    }

    @Override
    public int updateFileBizId(Long fileId, Long bizId) {
        return templateMapper.updateFileBizId(fileId, bizId);
    }

    @Override
    public Template save(Template template) {
        int inserted = templateMapper.insertTemplate(template);
        if (inserted <= 0 || template.getId() == null) {
            throw new IllegalStateException("template insert failed or id was not generated");
        }
        return template;
    }

    @Override
    public Optional<Template> findById(Long templateId) {
        return Optional.ofNullable(templateMapper.selectById(templateId));
    }

    @Override
    public List<Template> findPage(Long projectId, List<Long> accessibleProjectIds, String templateCategory, String templateType, String status, String keyword) {
        return templateMapper.selectPage(projectId, accessibleProjectIds, templateCategory, templateType, status, keyword);
    }

    @Override
    public int update(Template template) {
        return templateMapper.updateTemplate(template);
    }

    @Override
    public int updateStatus(Long templateId, String status) {
        return templateMapper.updateStatus(templateId, status);
    }

    @Override
    public int delete(Long templateId) {
        return templateMapper.logicalDelete(templateId);
    }
}
