package com.xd.smartworksite.datasource.facade;

import java.util.List;

public interface DataSourceScopeFacade {

    List<Long> validateEnabledDataSources(Long projectId, List<Long> dataSourceIds);
}
