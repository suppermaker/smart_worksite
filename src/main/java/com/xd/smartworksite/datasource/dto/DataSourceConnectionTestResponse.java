package com.xd.smartworksite.datasource.dto;

public class DataSourceConnectionTestResponse {
    private Long dataSourceId;
    private String dbType;
    private boolean success;
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private Integer validationTimeoutSeconds;
    private Long elapsedMs;

    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getDatabaseProductName() { return databaseProductName; }
    public void setDatabaseProductName(String databaseProductName) { this.databaseProductName = databaseProductName; }
    public String getDatabaseProductVersion() { return databaseProductVersion; }
    public void setDatabaseProductVersion(String databaseProductVersion) { this.databaseProductVersion = databaseProductVersion; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverVersion() { return driverVersion; }
    public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }
    public Integer getValidationTimeoutSeconds() { return validationTimeoutSeconds; }
    public void setValidationTimeoutSeconds(Integer validationTimeoutSeconds) { this.validationTimeoutSeconds = validationTimeoutSeconds; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
}
