package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.datasource.domain.BusinessDataSource;
import com.xd.smartworksite.datasource.domain.SqlSafetyResult;
import com.xd.smartworksite.datasource.dto.DatabaseQueryRequest;
import com.xd.smartworksite.datasource.dto.DatabaseQueryResponse;
import com.xd.smartworksite.datasource.infra.DatabaseQueryExecutor;
import com.xd.smartworksite.datasource.infra.ReadOnlySqlValidator;
import com.xd.smartworksite.datasource.infra.SqlSummary;
import com.xd.smartworksite.datasource.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

@Service
public class DatabaseQuestionApplicationService {

    private final DataSourceRepository dataSourceRepository;
    private final ReadOnlySqlValidator readOnlySqlValidator;
    private final DatabaseQueryExecutor databaseQueryExecutor;

    public DatabaseQuestionApplicationService(DataSourceRepository dataSourceRepository,
                                              ReadOnlySqlValidator readOnlySqlValidator,
                                              DatabaseQueryExecutor databaseQueryExecutor) {
        this.dataSourceRepository = dataSourceRepository;
        this.readOnlySqlValidator = readOnlySqlValidator;
        this.databaseQueryExecutor = databaseQueryExecutor;
    }

    public DatabaseQueryResponse query(DatabaseQueryRequest request) {
        BusinessDataSource dataSource = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Data source does not exist"));
        if (!dataSource.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Data source does not belong to project");
        }
        if (!dataSource.isEnabled()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Data source is disabled");
        }
        SqlSafetyResult safetyResult = readOnlySqlValidator.validate(request.getSql());
        DatabaseQueryExecutor.QueryExecutionResult executionResult = databaseQueryExecutor.dryRun(
                request.getPageNo(), request.getPageSize());
        DatabaseQueryResponse response = new DatabaseQueryResponse();
        response.setDataSourceId(dataSource.getId());
        response.setProjectId(dataSource.getProjectId());
        response.setSqlSummary(SqlSummary.summarize(safetyResult.getNormalizedSql()));
        response.setTables(safetyResult.getTableNames());
        response.setColumns(executionResult.columns());
        response.setRows(executionResult.rows());
        response.setPageNo(executionResult.pageNo());
        response.setPageSize(executionResult.pageSize());
        response.setCostMs(executionResult.costMs());
        response.setResultSummary("SQL validated as read-only; execution awaits datasource credential and whitelist contracts");
        return response;
    }
}
