package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.datasource.domain.DataSource;
import com.xd.smartworksite.datasource.dto.DataSourceConnectionTestResponse;
import com.xd.smartworksite.datasource.dto.DataSourceSchemaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class JdbcDataSourceInspector {
    private final DataSourcePasswordCipher passwordCipher;
    private final int validationTimeoutSeconds;
    private final int schemaTableLimit;
    private final int schemaColumnLimitPerTable;

    public JdbcDataSourceInspector(DataSourcePasswordCipher passwordCipher,
                                   @Value("${app.datasource.validation-timeout-seconds:5}") int validationTimeoutSeconds,
                                   @Value("${app.datasource.schema-table-limit:50}") int schemaTableLimit,
                                   @Value("${app.datasource.schema-column-limit-per-table:80}") int schemaColumnLimitPerTable) {
        this.passwordCipher = passwordCipher;
        this.validationTimeoutSeconds = Math.max(1, validationTimeoutSeconds);
        this.schemaTableLimit = Math.max(1, schemaTableLimit);
        this.schemaColumnLimitPerTable = Math.max(1, schemaColumnLimitPerTable);
    }

    public DataSourceConnectionTestResponse testConnection(DataSource dataSource) {
        Instant started = Instant.now();
        try (Connection connection = openConnection(dataSource)) {
            boolean valid = connection.isValid(validationTimeoutSeconds);
            if (!valid) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "data source connection validation failed");
            }
            DatabaseMetaData metaData = connection.getMetaData();
            DataSourceConnectionTestResponse response = new DataSourceConnectionTestResponse();
            response.setDataSourceId(dataSource.getId());
            response.setDbType(dataSource.getDbType());
            response.setSuccess(true);
            response.setDatabaseProductName(metaData.getDatabaseProductName());
            response.setDatabaseProductVersion(metaData.getDatabaseProductVersion());
            response.setDriverName(metaData.getDriverName());
            response.setDriverVersion(metaData.getDriverVersion());
            response.setValidationTimeoutSeconds(validationTimeoutSeconds);
            response.setElapsedMs(Duration.between(started, Instant.now()).toMillis());
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "data source connection test failed: " + ex.getMessage());
        }
    }

    public DataSourceSchemaResponse inspectSchema(DataSource dataSource) {
        try (Connection connection = openConnection(dataSource)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = resolveSchema(dataSource, connection);
            DataSourceSchemaResponse response = new DataSourceSchemaResponse();
            response.setDataSourceId(dataSource.getId());
            response.setDbType(dataSource.getDbType());
            response.setCatalog(catalog);
            response.setSchema(schema);
            response.setTables(readTables(metaData, catalog, schema));
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "data source schema inspection failed: " + ex.getMessage());
        }
    }

    private Connection openConnection(DataSource dataSource) throws SQLException {
        String password = passwordCipher.decrypt(dataSource.getPasswordCipher());
        Connection connection = DriverManager.getConnection(dataSource.getJdbcUrl(), dataSource.getUsername(), password);
        connection.setReadOnly(true);
        return connection;
    }

    private String resolveSchema(DataSource dataSource, Connection connection) throws SQLException {
        String dbType = dataSource.getDbType() == null ? "" : dataSource.getDbType().toUpperCase(Locale.ROOT);
        if ("MYSQL".equals(dbType)) {
            return null;
        }
        return connection.getSchema();
    }

    private List<DataSourceSchemaResponse.Table> readTables(DatabaseMetaData metaData, String catalog, String schema) throws SQLException {
        List<DataSourceSchemaResponse.Table> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next() && tables.size() < schemaTableLimit) {
                DataSourceSchemaResponse.Table table = new DataSourceSchemaResponse.Table();
                table.setTableName(rs.getString("TABLE_NAME"));
                table.setTableType(rs.getString("TABLE_TYPE"));
                table.setRemarks(rs.getString("REMARKS"));
                table.setColumns(readColumns(metaData, catalog, schema, table.getTableName()));
                tables.add(table);
            }
        }
        return tables;
    }

    private List<DataSourceSchemaResponse.Column> readColumns(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<DataSourceSchemaResponse.Column> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next() && columns.size() < schemaColumnLimitPerTable) {
                DataSourceSchemaResponse.Column column = new DataSourceSchemaResponse.Column();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setTypeName(rs.getString("TYPE_NAME"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setRemarks(rs.getString("REMARKS"));
                columns.add(column);
            }
        }
        return columns;
    }
}
