package com.xd.smartworksite.auth.application;

import com.xd.smartworksite.auth.domain.ProjectMember;
import com.xd.smartworksite.auth.dto.ProjectMemberCreateRequest;
import com.xd.smartworksite.auth.dto.ProjectMemberResponse;
import com.xd.smartworksite.auth.mapper.ProjectMemberMapper;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectMemberApplicationService {

    private final ProjectMemberMapper projectMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final ProjectAccessApplicationService projectAccessApplicationService;

    public ProjectMemberApplicationService(ProjectMemberMapper projectMemberMapper,
                                           UserAccountMapper userAccountMapper,
                                           ProjectAccessApplicationService projectAccessApplicationService) {
        this.projectMemberMapper = projectMemberMapper;
        this.userAccountMapper = userAccountMapper;
        this.projectAccessApplicationService = projectAccessApplicationService;
    }

    public List<ProjectMemberResponse> listMembers(Long projectId) {
        projectAccessApplicationService.requireProjectAccess(projectId);
        return projectMemberMapper.selectByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(Long projectId, ProjectMemberCreateRequest request) {
        projectAccessApplicationService.requireProjectWritableManage(projectId);
        if (userAccountMapper.selectById(request.getUserId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (projectMemberMapper.selectByProjectIdAndUserId(projectId, request.getUserId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已是项目成员");
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(request.getUserId());
        member.setProjectRole(request.getProjectRole());
        member.setStatus("ENABLED");
        requireUpdated(projectMemberMapper.insert(member), "project member create failed");
        ProjectMember inserted = projectMemberMapper.selectByProjectIdAndUserId(projectId, request.getUserId());
        if (inserted == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "project member create readback failed");
        }
        return toResponse(inserted);
    }

    @Transactional
    public ProjectMemberResponse updateMember(Long projectId, Long userId, ProjectMemberCreateRequest request) {
        projectAccessApplicationService.requireProjectWritableManage(projectId);
        ProjectMember member = projectMemberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null) throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        member.setProjectRole(request.getProjectRole());
        requireUpdated(projectMemberMapper.update(member), "project member update failed");
        ProjectMember updated = projectMemberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (updated == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "project member update readback failed");
        }
        return toResponse(updated);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        projectAccessApplicationService.requireProjectWritableManage(projectId);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能移除自己");
        }
        if (projectMemberMapper.selectByProjectIdAndUserId(projectId, userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        }
        requireUpdated(projectMemberMapper.deleteByProjectIdAndUserId(projectId, userId, currentUserId), "project member delete failed");
    }

    public void requireProjectMember(Long projectId) {
        projectAccessApplicationService.requireProjectAccess(projectId);
    }

    private ProjectMemberResponse toResponse(ProjectMember m) {
        ProjectMemberResponse r = new ProjectMemberResponse();
        r.setId(m.getId());
        r.setProjectId(m.getProjectId());
        r.setUserId(m.getUserId());
        r.setUsername(m.getUsername());
        r.setDisplayName(m.getDisplayName());
        r.setProjectRole(m.getProjectRole());
        r.setStatus(m.getStatus());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }

    private void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }
}
