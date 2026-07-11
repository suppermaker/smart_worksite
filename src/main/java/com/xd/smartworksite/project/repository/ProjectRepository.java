package com.xd.smartworksite.project.repository;

import com.xd.smartworksite.project.domain.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    List<Project> findPage(String keyword, String status);

    List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds);

    Optional<Project> findById(Long projectId);

    Optional<Project> findByProjectCode(String projectCode);

    Project insert(Project project);

    int update(Project project);

    int softDelete(Long projectId, Long updatedBy);

    int updateStatus(Long projectId, String status, Long updatedBy);

    int updateSettings(Long projectId, String settings, Long updatedBy);

    long countActiveMembers(Long projectId);

    long countKnowledgeBases(Long projectId);

    long countReports(Long projectId);

    long countDataSources(Long projectId);

    long countQaMessages(Long projectId);

    long countReviewRecords(Long projectId);

    long countOcrRecords(Long projectId);

    long sumFileStorageBytes(Long projectId);
}
