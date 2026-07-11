package com.xd.smartworksite.audit.mapper;

import com.xd.smartworksite.audit.domain.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogMapper {
    int insert(AuditLog log);

    List<AuditLog> selectPage(@Param("projectId") Long projectId,
                              @Param("accessibleProjectIds") List<Long> accessibleProjectIds,
                              @Param("operatorId") Long operatorId,
                              @Param("action") String action,
                              @Param("objectType") String objectType,
                              @Param("createdFrom") LocalDateTime createdFrom,
                              @Param("createdTo") LocalDateTime createdTo);
}
