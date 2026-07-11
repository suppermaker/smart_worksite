package com.xd.smartworksite.datasource.application;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.datasource.domain.DataSource;
import com.xd.smartworksite.datasource.domain.DataSourceStatus;
import com.xd.smartworksite.datasource.domain.DbType;
import com.xd.smartworksite.datasource.dto.DataSourceConnectionTestResponse;
import com.xd.smartworksite.datasource.dto.DataSourceCreateRequest;
import com.xd.smartworksite.datasource.dto.DataSourceQueryRequest;
import com.xd.smartworksite.datasource.dto.DataSourceResponse;
import com.xd.smartworksite.datasource.dto.DataSourceSchemaResponse;
import com.xd.smartworksite.datasource.dto.DataSourceUpdateRequest;
import com.xd.smartworksite.datasource.repository.DataSourceRepository;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class DataSourceApplicationService {
    private final DataSourceRepository dataSourceRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final DataSourcePasswordCipher passwordCipher;
    private final JdbcDataSourceInspector jdbcDataSourceInspector;

    public DataSourceApplicationService(DataSourceRepository dataSourceRepository,
                                        ProjectAccessApplicationService projectAccessApplicationService,
                                        DataSourcePasswordCipher passwordCipher,
                                        JdbcDataSourceInspector jdbcDataSourceInspector) {
        this.dataSourceRepository = dataSourceRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.passwordCipher = passwordCipher;
        this.jdbcDataSourceInspector = jdbcDataSourceInspector;
    }

    @Transactional
    public DataSourceResponse createDataSource(DataSourceCreateRequest request) {
        projectAccessApplicationService.requireProjectWritableManage(request.getProjectId());
        DataSource dataSource = new DataSource();
        dataSource.setProjectId(request.getProjectId());
        dataSource.setName(normalizeRequired(request.getName(), "name is required"));
        dataSource.setDbType(normalizeDbType(request.getDbType()));
        dataSource.setJdbcUrl(normalizeRequired(request.getJdbcUrl(), "jdbcUrl is required"));
        dataSource.setUsername(normalizeRequired(request.getUsername(), "username is required"));
        dataSource.setPasswordCipher(passwordCipher.encrypt(request.getPassword()));
        dataSource.setStatus(DataSourceStatus.ENABLED.name());
        dataSource.setCreatedBy(SecurityUtils.getCurrentUserId());
        dataSource.setUpdatedBy(SecurityUtils.getCurrentUserId());
        dataSourceRepository.insert(dataSource);
        return getDataSource(dataSource.getId());
    }

    public PageResult<DataSourceResponse> queryDataSources(DataSourceQueryRequest request) {
        List<Long> accessibleProjectIds = null;
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        } else if (!SecurityUtils.isPlatformAdmin()) {
            accessibleProjectIds = projectAccessApplicationService.currentUserAccessibleProjectIds();
            if (accessibleProjectIds.isEmpty()) {
                return new PageResult<>(request.getPageNo(), request.getPageSize(), 0, List.of());
            }
        }
        List<Long> finalAccessibleProjectIds = accessibleProjectIds;
        Page<DataSource> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> dataSourceRepository.findPage(
                        request.getProjectId(),
                        finalAccessibleProjectIds,
                        normalizeOptionalDbType(request.getDbType()),
                        normalizeOptionalStatus(request.getStatus()),
                        trimToNull(request.getKeyword())
                ));
        return new PageResult<>(
                request.getPageNo(),
                request.getPageSize(),
                page.getTotal(),
                page.getResult().stream().map(this::toResponse).toList()
        );
    }

    public DataSourceResponse getDataSource(Long dataSourceId) {
        DataSource dataSource = requireDataSourceAccess(dataSourceId);
        return toResponse(dataSource);
    }

    @Transactional
    public DataSourceResponse updateDataSource(Long dataSourceId, DataSourceUpdateRequest request) {
        DataSource current = requireDataSourceManage(dataSourceId);
        current.setName(normalizeRequired(request.getName(), "name is required"));
        current.setDbType(normalizeDbType(request.getDbType()));
        current.setJdbcUrl(normalizeRequired(request.getJdbcUrl(), "jdbcUrl is required"));
        current.setUsername(normalizeRequired(request.getUsername(), "username is required"));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            current.setPasswordCipher(passwordCipher.encrypt(request.getPassword()));
        }
        current.setUpdatedBy(SecurityUtils.getCurrentUserId());
        int updated = dataSourceRepository.update(current);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "data source update failed");
        }
        return getDataSource(dataSourceId);
    }

    @Transactional
    public DataSourceResponse enableDataSource(Long dataSourceId) {
        return updateStatus(dataSourceId, DataSourceStatus.ENABLED);
    }

    @Transactional
    public DataSourceResponse disableDataSource(Long dataSourceId) {
        return updateStatus(dataSourceId, DataSourceStatus.DISABLED);
    }

    @Transactional
    public void deleteDataSource(Long dataSourceId) {
        requireDataSourceManage(dataSourceId);
        int updated = dataSourceRepository.softDelete(dataSourceId, SecurityUtils.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "data source not found");
        }
    }

    public DataSourceConnectionTestResponse testConnection(Long dataSourceId) {
        DataSource dataSource = requireDataSourceAccess(dataSourceId);
        projectAccessApplicationService.requireProjectWritableAccess(dataSource.getProjectId());
        return jdbcDataSourceInspector.testConnection(dataSource);
    }

    public DataSourceSchemaResponse inspectSchema(Long dataSourceId) {
        DataSource dataSource = requireDataSourceAccess(dataSourceId);
        projectAccessApplicationService.requireProjectWritableAccess(dataSource.getProjectId());
        if (!DataSourceStatus.ENABLED.name().equals(dataSource.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "data source is not enabled");
        }
        return jdbcDataSourceInspector.inspectSchema(dataSource);
    }

    private DataSourceResponse updateStatus(Long dataSourceId, DataSourceStatus status) {
        requireDataSourceManage(dataSourceId);
        int updated = dataSourceRepository.updateStatus(dataSourceId, status.name(), SecurityUtils.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "data source not found");
        }
        return getDataSource(dataSourceId);
    }

    private DataSource requireDataSourceAccess(Long dataSourceId) {
        DataSource dataSource = requireDataSource(dataSourceId);
        projectAccessApplicationService.requireProjectAccess(dataSource.getProjectId());
        return dataSource;
    }

    private DataSource requireDataSourceManage(Long dataSourceId) {
        DataSource dataSource = requireDataSource(dataSourceId);
        projectAccessApplicationService.requireProjectWritableManage(dataSource.getProjectId());
        return dataSource;
    }

    private DataSource requireDataSource(Long dataSourceId) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "dataSourceId is required");
        }
        return dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "data source not found"));
    }

    private String normalizeDbType(String dbType) {
        String normalized = normalizeRequired(dbType, "dbType is required").toUpperCase(Locale.ROOT);
        try {
            return DbType.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "dbType must be MYSQL, POSTGRESQL or KINGBASE");
        }
    }

    private String normalizeOptionalDbType(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return null;
        }
        return normalizeDbType(dbType);
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return DataSourceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ENABLED or DISABLED");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private DataSourceResponse toResponse(DataSource dataSource) {
        DataSourceResponse response = new DataSourceResponse();
        response.setDataSourceId(dataSource.getId());
        response.setProjectId(dataSource.getProjectId());
        response.setName(dataSource.getName());
        response.setDbType(dataSource.getDbType());
        response.setJdbcUrl(dataSource.getJdbcUrl());
        response.setUsername(dataSource.getUsername());
        response.setStatus(dataSource.getStatus());
        response.setCreatedAt(dataSource.getCreatedAt());
        response.setUpdatedAt(dataSource.getUpdatedAt());
        return response;
    }
}
