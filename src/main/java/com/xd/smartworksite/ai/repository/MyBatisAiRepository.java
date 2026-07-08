package com.xd.smartworksite.ai.repository;

import com.xd.smartworksite.ai.domain.DataSourceRecord;
import com.xd.smartworksite.ai.domain.ExternalCallLog;
import com.xd.smartworksite.ai.mapper.AiMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MyBatisAiRepository implements AiRepository {
    private final AiMapper aiMapper;

    public MyBatisAiRepository(AiMapper aiMapper) {
        this.aiMapper = aiMapper;
    }

    @Override
    public void saveExternalCallLog(ExternalCallLog log) {
        aiMapper.insertExternalCallLog(log);
    }

    @Override
    public List<ExternalCallLog> queryExternalCallLogs(Long projectId, String serviceName, String callType, String status) {
        return aiMapper.selectExternalCallLogPage(projectId, serviceName, callType, status);
    }

    @Override
    public DataSourceRecord findEnabledDataSource(Long projectId, Long dataSourceId) {
        return aiMapper.selectDataSourceById(projectId, dataSourceId);
    }
}
