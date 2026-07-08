package com.xd.smartworksite.ai.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseQueryResponse {
    private String sql;
    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private String summary;
    private List<String> warnings = new ArrayList<>();
    private String providerTraceId;
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
}
