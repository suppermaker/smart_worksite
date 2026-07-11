package com.xd.smartworksite.template.mapper;

import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TemplateMapper {

    int insertFileObject(FileObjectRecord fileObject);

    FileObjectRecord selectFileObjectById(@Param("fileId") Long fileId);

    int updateFileBizId(@Param("fileId") Long fileId, @Param("bizId") Long bizId);

    int insertTemplate(Template template);

    Template selectById(@Param("templateId") Long templateId);

    List<Template> selectPage(@Param("projectId") Long projectId,
                              @Param("accessibleProjectIds") List<Long> accessibleProjectIds,
                              @Param("templateCategory") String templateCategory,
                              @Param("templateType") String templateType,
                              @Param("status") String status,
                              @Param("keyword") String keyword);

    int updateTemplate(Template template);

    int updateStatus(@Param("templateId") Long templateId, @Param("status") String status);

    int logicalDelete(@Param("templateId") Long templateId);
}
