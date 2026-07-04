package com.xd.smartworksite.datasource.mapper;

import com.xd.smartworksite.datasource.domain.BusinessDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataSourceMapper {

    BusinessDataSource selectById(@Param("dataSourceId") Long dataSourceId);
}
