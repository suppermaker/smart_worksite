package com.xd.smartworksite.auth.mapper;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper {

    List<Role> selectAll(@Param("keyword") String keyword);

    Role selectById(@Param("roleId") Long roleId);

    Role selectByRoleCode(@Param("roleCode") String roleCode);

    int insert(@Param("role") Role role, @Param("operatorId") Long operatorId);

    int update(@Param("role") Role role, @Param("operatorId") Long operatorId);

    int updateStatus(@Param("roleId") Long roleId, @Param("status") String status,
                     @Param("operatorId") Long operatorId);

    int softDelete(@Param("roleId") Long roleId, @Param("operatorId") Long operatorId);

    int countActiveUsersByRoleId(@Param("roleId") Long roleId);

    List<Permission> selectAllPermissions();

    Permission selectPermissionById(@Param("permissionId") Long permissionId);

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId,
                             @Param("operatorId") Long operatorId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    List<String> selectUserRoleCodes(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId,
                       @Param("operatorId") Long operatorId);

    int deleteUserRoles(@Param("userId") Long userId);
}
