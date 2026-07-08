package com.xd.smartworksite.ai.repository;

import com.xd.smartworksite.ai.domain.DataSourceRecord;
import com.xd.smartworksite.ai.domain.ExternalCallLog;

import java.util.List;

public interface AiRepository {
    void saveExternalCallLog(ExternalCallLog log);

    List<ExternalCallLog> queryExternalCallLogs(Long projectId, String serviceName, String callType, String status);

    DataSourceRecord findEnabledDataSource(Long projectId, Long dataSourceId);
}
