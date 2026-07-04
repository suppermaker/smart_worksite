package com.xd.smartworksite.datasource.domain;

import java.util.List;

public class SqlSafetyResult {

    private final String normalizedSql;
    private final List<String> tableNames;

    public SqlSafetyResult(String normalizedSql, List<String> tableNames) {
        this.normalizedSql = normalizedSql;
        this.tableNames = List.copyOf(tableNames);
    }

    public String getNormalizedSql() {
        return normalizedSql;
    }

    public List<String> getTableNames() {
        return tableNames;
    }
}
