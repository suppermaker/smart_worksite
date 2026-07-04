package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.datasource.domain.BusinessDataSource;
import com.xd.smartworksite.datasource.facade.DataSourceScopeFacade;
import com.xd.smartworksite.datasource.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataSourceScopeApplicationService implements DataSourceScopeFacade {

    private final DataSourceRepository dataSourceRepository;

    public DataSourceScopeApplicationService(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @Override
    public List<Long> validateEnabledDataSources(Long projectId, List<Long> dataSourceIds) {
        List<Long> requestedIds = normalizeIds(dataSourceIds);
        for (Long dataSourceId : requestedIds) {
            BusinessDataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Data source does not exist"));
            if (!dataSource.getProjectId().equals(projectId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Data source does not belong to project");
            }
            if (!dataSource.isEnabled()) {
                throw new BusinessException(ErrorCode.CONFLICT, "Data source is disabled");
            }
        }
        return requestedIds;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Data source id must not be null");
            }
        }
        return ids.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
