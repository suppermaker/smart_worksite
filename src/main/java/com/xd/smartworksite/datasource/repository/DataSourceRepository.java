package com.xd.smartworksite.datasource.repository;

import com.xd.smartworksite.datasource.domain.BusinessDataSource;

import java.util.Optional;

public interface DataSourceRepository {

    Optional<BusinessDataSource> findById(Long dataSourceId);
}
