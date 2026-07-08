package com.xd.smartworksite.ai.mapper;

import com.xd.smartworksite.ai.domain.DataSourceRecord;
import com.xd.smartworksite.ai.domain.ExternalCallLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiMapper {
    void insertExternalCallLog(ExternalCallLog log);

    List<ExternalCallLog> selectExternalCallLogPage(@Param("projectId") Long projectId,
                                                    @Param("serviceName") String serviceName,
                                                    @Param("callType") String callType,
                                                    @Param("status") String status);

    DataSourceRecord selectDataSourceById(@Param("projectId") Long projectId,
                                           @Param("dataSourceId") Long dataSourceId);
}
