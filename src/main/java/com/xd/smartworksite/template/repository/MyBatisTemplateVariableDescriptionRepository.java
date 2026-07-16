package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.TemplateVariableDescription;
import com.xd.smartworksite.template.mapper.TemplateVariableDescriptionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTemplateVariableDescriptionRepository implements TemplateVariableDescriptionRepository {

    private final TemplateVariableDescriptionMapper mapper;

    public MyBatisTemplateVariableDescriptionRepository(TemplateVariableDescriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TemplateVariableDescription> findByKey(Long templateId, Long fileId, String variableName) {
        return Optional.ofNullable(mapper.selectByKey(templateId, fileId, variableName));
    }

    @Override
    public List<TemplateVariableDescription> findActiveByTemplateAndFile(Long templateId, Long fileId) {
        return mapper.selectActiveByTemplateAndFile(templateId, fileId);
    }

    @Override
    public int insert(TemplateVariableDescription description) {
        return mapper.insert(description);
    }

    @Override
    public int updateAndReactivate(TemplateVariableDescription description) {
        return mapper.updateAndReactivate(description);
    }
}
