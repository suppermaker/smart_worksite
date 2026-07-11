package com.xd.smartworksite.project.mapper;

import com.xd.smartworksite.project.domain.Project;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectMapper {

    List<Project> selectPage(@Param("keyword") String keyword, @Param("status") String status);

    List<Project> selectPageByProjectIds(@Param("keyword") String keyword,
                                         @Param("status") String status,
                                         @Param("projectIds") List<Long> projectIds);

    Project selectById(@Param("projectId") Long projectId);

    Project selectByProjectCode(@Param("projectCode") String projectCode);

    int insert(Project project);

    int update(Project project);

    int softDelete(@Param("projectId") Long projectId, @Param("updatedBy") Long updatedBy);

    int updateStatus(@Param("projectId") Long projectId, @Param("status") String status, @Param("updatedBy") Long updatedBy);

    int updateSettings(@Param("projectId") Long projectId,
                       @Param("settings") String settings,
                       @Param("updatedBy") Long updatedBy);

    long countActiveMembers(@Param("projectId") Long projectId);

    long countKnowledgeBases(@Param("projectId") Long projectId);

    long countReports(@Param("projectId") Long projectId);

    long countDataSources(@Param("projectId") Long projectId);

    long countQaMessages(@Param("projectId") Long projectId);

    long countReviewRecords(@Param("projectId") Long projectId);

    long countOcrRecords(@Param("projectId") Long projectId);

    long sumFileStorageBytes(@Param("projectId") Long projectId);
}
