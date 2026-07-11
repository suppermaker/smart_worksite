package com.xd.smartworksite.datasource.repository;

import com.xd.smartworksite.datasource.domain.DataSource;

import java.util.List;
import java.util.Optional;

public interface DataSourceRepository {
    DataSource insert(DataSource dataSource);

    Optional<DataSource> findById(Long dataSourceId);

    List<DataSource> findPage(Long projectId, List<Long> accessibleProjectIds, String dbType, String status, String keyword);

    int update(DataSource dataSource);

    int updateStatus(Long dataSourceId, String status, Long updatedBy);

    int softDelete(Long dataSourceId, Long updatedBy);
}
