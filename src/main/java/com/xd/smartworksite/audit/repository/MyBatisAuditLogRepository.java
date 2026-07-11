package com.xd.smartworksite.audit.repository;

import com.xd.smartworksite.audit.domain.AuditLog;
import com.xd.smartworksite.audit.mapper.AuditLogMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MyBatisAuditLogRepository implements AuditLogRepository {
    private final AuditLogMapper auditLogMapper;

    public MyBatisAuditLogRepository(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public int insert(AuditLog log) {
        return auditLogMapper.insert(log);
    }

    @Override
    public List<AuditLog> findPage(Long projectId, List<Long> accessibleProjectIds, Long operatorId,
                                   String action, String objectType, LocalDateTime createdFrom, LocalDateTime createdTo) {
        return auditLogMapper.selectPage(projectId, accessibleProjectIds, operatorId, action, objectType, createdFrom, createdTo);
    }
}
