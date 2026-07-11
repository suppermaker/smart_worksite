package com.xd.smartworksite.project.repository;

import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.mapper.ProjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisProjectRepository implements ProjectRepository {

    private final ProjectMapper projectMapper;

    public MyBatisProjectRepository(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public List<Project> findPage(String keyword, String status) {
        return projectMapper.selectPage(keyword, status);
    }

    @Override
    public List<Project> findPageByProjectIds(String keyword, String status, List<Long> projectIds) {
        return projectMapper.selectPageByProjectIds(keyword, status, projectIds);
    }

    @Override
    public Optional<Project> findById(Long projectId) {
        return Optional.ofNullable(projectMapper.selectById(projectId));
    }

    @Override
    public Optional<Project> findByProjectCode(String projectCode) {
        return Optional.ofNullable(projectMapper.selectByProjectCode(projectCode));
    }

    @Override
    public Project insert(Project project) {
        int inserted = projectMapper.insert(project);
        if (inserted <= 0 || project.getId() == null) {
            throw new IllegalStateException("project insert failed or id was not generated");
        }
        return project;
    }

    @Override
    public int update(Project project) {
        return projectMapper.update(project);
    }

    @Override
    public int softDelete(Long projectId, Long updatedBy) {
        return projectMapper.softDelete(projectId, updatedBy);
    }

    @Override
    public int updateStatus(Long projectId, String status, Long updatedBy) {
        return projectMapper.updateStatus(projectId, status, updatedBy);
    }

    @Override
    public int updateSettings(Long projectId, String settings, Long updatedBy) {
        return projectMapper.updateSettings(projectId, settings, updatedBy);
    }

    @Override
    public long countActiveMembers(Long projectId) { return projectMapper.countActiveMembers(projectId); }

    @Override
    public long countKnowledgeBases(Long projectId) { return projectMapper.countKnowledgeBases(projectId); }

    @Override
    public long countReports(Long projectId) { return projectMapper.countReports(projectId); }

    @Override
    public long countDataSources(Long projectId) { return projectMapper.countDataSources(projectId); }

    @Override
    public long countQaMessages(Long projectId) { return projectMapper.countQaMessages(projectId); }

    @Override
    public long countReviewRecords(Long projectId) { return projectMapper.countReviewRecords(projectId); }

    @Override
    public long countOcrRecords(Long projectId) { return projectMapper.countOcrRecords(projectId); }

    @Override
    public long sumFileStorageBytes(Long projectId) { return projectMapper.sumFileStorageBytes(projectId); }
}
