package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {

    FileObjectRecord saveFileObject(FileObjectRecord fileObject);

    Optional<FileObjectRecord> findFileObjectById(Long fileId);

    int updateFileBizId(Long fileId, Long bizId);

    Template save(Template template);

    Optional<Template> findById(Long templateId);

    List<Template> findPage(Long projectId, List<Long> accessibleProjectIds, String templateCategory, String templateType, String status, String keyword);

    int update(Template template);

    int updateStatus(Long templateId, String status);

    int delete(Long templateId);
}
