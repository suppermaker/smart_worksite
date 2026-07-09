package com.xd.smartworksite.auth.mapper;

import com.xd.smartworksite.auth.domain.Permission;
import com.xd.smartworksite.auth.domain.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper {

    List<Role> selectAll(@Param("keyword") String keyword);

    Role selectById(@Param("roleId") Long roleId);

    List<Permission> selectAllPermissions();

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId,
                             @Param("operatorId") Long operatorId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    List<String> selectUserRoleCodes(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId,
                       @Param("operatorId") Long operatorId);

    int deleteUserRoles(@Param("userId") Long userId);
}
