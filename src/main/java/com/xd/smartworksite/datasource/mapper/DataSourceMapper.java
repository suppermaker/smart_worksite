package com.xd.smartworksite.datasource.mapper;

import com.xd.smartworksite.datasource.domain.DataSource;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DataSourceMapper {
    int insert(DataSource dataSource);

    DataSource selectById(@Param("dataSourceId") Long dataSourceId);

    List<DataSource> selectPage(@Param("projectId") Long projectId,
                                @Param("accessibleProjectIds") List<Long> accessibleProjectIds,
                                @Param("dbType") String dbType,
                                @Param("status") String status,
                                @Param("keyword") String keyword);

    int update(DataSource dataSource);

    int updateStatus(@Param("dataSourceId") Long dataSourceId,
                     @Param("status") String status,
                     @Param("updatedBy") Long updatedBy);

    int softDelete(@Param("dataSourceId") Long dataSourceId,
                   @Param("updatedBy") Long updatedBy);
}
