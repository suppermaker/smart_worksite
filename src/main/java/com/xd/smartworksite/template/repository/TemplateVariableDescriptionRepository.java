package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.TemplateVariableDescription;

import java.util.List;
import java.util.Optional;

public interface TemplateVariableDescriptionRepository {

    Optional<TemplateVariableDescription> findByKey(Long templateId, Long fileId, String variableName);

    List<TemplateVariableDescription> findActiveByTemplateAndFile(Long templateId, Long fileId);

    int insert(TemplateVariableDescription description);

    int updateAndReactivate(TemplateVariableDescription description);
}
