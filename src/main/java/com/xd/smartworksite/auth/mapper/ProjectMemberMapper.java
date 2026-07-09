package com.xd.smartworksite.auth.mapper;

import com.xd.smartworksite.auth.domain.ProjectMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectMemberMapper {

    List<ProjectMember> selectByProjectId(@Param("projectId") Long projectId);

    ProjectMember selectByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    int countActiveMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    int insert(ProjectMember member);

    int update(ProjectMember member);

    int deleteByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId,
                                   @Param("operatorId") Long operatorId);

    List<Long> selectProjectIdsByUserId(@Param("userId") Long userId);
}
