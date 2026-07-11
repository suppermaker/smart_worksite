package com.xd.smartworksite.datasource.repository;

import com.xd.smartworksite.datasource.domain.DataSource;
import com.xd.smartworksite.datasource.mapper.DataSourceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisDataSourceRepository implements DataSourceRepository {
    private final DataSourceMapper dataSourceMapper;

    public MyBatisDataSourceRepository(DataSourceMapper dataSourceMapper) {
        this.dataSourceMapper = dataSourceMapper;
    }

    @Override
    public DataSource insert(DataSource dataSource) {
        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    @Override
    public Optional<DataSource> findById(Long dataSourceId) {
        return Optional.ofNullable(dataSourceMapper.selectById(dataSourceId));
    }

    @Override
    public List<DataSource> findPage(Long projectId, List<Long> accessibleProjectIds, String dbType, String status, String keyword) {
        return dataSourceMapper.selectPage(projectId, accessibleProjectIds, dbType, status, keyword);
    }

    @Override
    public int update(DataSource dataSource) {
        return dataSourceMapper.update(dataSource);
    }

    @Override
    public int updateStatus(Long dataSourceId, String status, Long updatedBy) {
        return dataSourceMapper.updateStatus(dataSourceId, status, updatedBy);
    }

    @Override
    public int softDelete(Long dataSourceId, Long updatedBy) {
        return dataSourceMapper.softDelete(dataSourceId, updatedBy);
    }
}
