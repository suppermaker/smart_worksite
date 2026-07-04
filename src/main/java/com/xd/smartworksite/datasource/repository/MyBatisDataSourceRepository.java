package com.xd.smartworksite.datasource.repository;

import com.xd.smartworksite.datasource.domain.BusinessDataSource;
import com.xd.smartworksite.datasource.mapper.DataSourceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MyBatisDataSourceRepository implements DataSourceRepository {

    private final DataSourceMapper dataSourceMapper;

    public MyBatisDataSourceRepository(DataSourceMapper dataSourceMapper) {
        this.dataSourceMapper = dataSourceMapper;
    }

    @Override
    public Optional<BusinessDataSource> findById(Long dataSourceId) {
        return Optional.ofNullable(dataSourceMapper.selectById(dataSourceId));
    }
}
