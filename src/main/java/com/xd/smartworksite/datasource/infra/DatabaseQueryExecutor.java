package com.xd.smartworksite.datasource.infra;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseQueryExecutor {

    public QueryExecutionResult dryRun(int pageNo, int pageSize) {
        return new QueryExecutionResult(List.of(), List.of(), 0L, pageNo, pageSize);
    }

    public record QueryExecutionResult(List<String> columns,
                                       List<Map<String, Object>> rows,
                                       Long costMs,
                                       Integer pageNo,
                                       Integer pageSize) {
    }
}
