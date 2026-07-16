package com.xd.smartworksite.template.mapper;

import com.xd.smartworksite.template.domain.TemplateVariableDescription;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TemplateVariableDescriptionMapper {

    TemplateVariableDescription selectByKey(@Param("templateId") Long templateId,
                                            @Param("fileId") Long fileId,
                                            @Param("variableName") String variableName);

    List<TemplateVariableDescription> selectActiveByTemplateAndFile(@Param("templateId") Long templateId,
                                                                    @Param("fileId") Long fileId);

    int insert(TemplateVariableDescription description);

    int updateAndReactivate(TemplateVariableDescription description);
}
