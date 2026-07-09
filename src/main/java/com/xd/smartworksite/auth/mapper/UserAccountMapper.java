package com.xd.smartworksite.auth.mapper;

import com.xd.smartworksite.auth.domain.UserAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAccountMapper {

    UserAccount selectByUsername(@Param("username") String username);

    UserAccount selectById(@Param("userId") Long userId);

    List<UserAccount> selectPage(@Param("keyword") String keyword, @Param("status") String status);

    int insert(UserAccount user);

    int update(UserAccount user);

    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    int updateStatus(@Param("userId") Long userId, @Param("status") String status);

    int updateLastLoginAt(@Param("userId") Long userId);

    List<String> selectRoleCodes(@Param("userId") Long userId);

    List<String> selectPermissionCodes(@Param("userId") Long userId);

    Long selectDefaultProjectId(@Param("userId") Long userId);
}
