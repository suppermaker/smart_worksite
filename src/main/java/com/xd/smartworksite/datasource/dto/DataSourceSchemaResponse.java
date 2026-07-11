package com.xd.smartworksite.datasource.dto;

import java.util.ArrayList;
import java.util.List;

public class DataSourceSchemaResponse {
    private Long dataSourceId;
    private String dbType;
    private String catalog;
    private String schema;
    private List<Table> tables = new ArrayList<>();

    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public String getCatalog() { return catalog; }
    public void setCatalog(String catalog) { this.catalog = catalog; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public List<Table> getTables() { return tables; }
    public void setTables(List<Table> tables) { this.tables = tables; }

    public static class Table {
        private String tableName;
        private String tableType;
        private String remarks;
        private List<Column> columns = new ArrayList<>();

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public List<Column> getColumns() { return columns; }
        public void setColumns(List<Column> columns) { this.columns = columns; }
    }

    public static class Column {
        private String columnName;
        private String typeName;
        private Integer columnSize;
        private Integer decimalDigits;
        private Boolean nullable;
        private String remarks;

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public Integer getColumnSize() { return columnSize; }
        public void setColumnSize(Integer columnSize) { this.columnSize = columnSize; }
        public Integer getDecimalDigits() { return decimalDigits; }
        public void setDecimalDigits(Integer decimalDigits) { this.decimalDigits = decimalDigits; }
        public Boolean getNullable() { return nullable; }
        public void setNullable(Boolean nullable) { this.nullable = nullable; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}
