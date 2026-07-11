package com.xd.smartworksite.audit.repository;

import com.xd.smartworksite.audit.domain.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository {
    int insert(AuditLog log);

    List<AuditLog> findPage(Long projectId, List<Long> accessibleProjectIds, Long operatorId,
                            String action, String objectType, LocalDateTime createdFrom, LocalDateTime createdTo);
}
