package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.ai.infra.AiPythonServiceProperties;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.datasource.domain.DataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcDataSourceInspectorTest {
    private static final String KEY = "1234567890123456";

    @Test
    void testConnectionAndInspectSchemaUseRealJdbcMetadata() throws Exception {
        String dbName = "ds_inspector_" + System.nanoTime();
        String url = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "secret");
             Statement statement = connection.createStatement()) {
            statement.execute("create table safety_event (id bigint primary key, title varchar(64), risk_level varchar(16))");
        }
        JdbcDataSourceInspector inspector = new JdbcDataSourceInspector(cipher(), 2, 20, 20);
        DataSource dataSource = dataSource(url, cipher().encrypt("secret"));

        var connectionResult = inspector.testConnection(dataSource);
        var schema = inspector.inspectSchema(dataSource);

        assertThat(connectionResult.isSuccess()).isTrue();
        assertThat(connectionResult.getDatabaseProductName()).containsIgnoringCase("H2");
        assertThat(schema.getTables()).anySatisfy(table -> {
            assertThat(table.getTableName()).isEqualToIgnoringCase("SAFETY_EVENT");
            assertThat(table.getColumns()).extracting("columnName")
                    .anySatisfy(name -> assertThat(String.valueOf(name)).isEqualToIgnoringCase("TITLE"));
        });
    }

    @Test
    void invalidJdbcUrlFailsVisibly() {
        JdbcDataSourceInspector inspector = new JdbcDataSourceInspector(cipher(), 1, 10, 10);
        DataSource dataSource = dataSource("jdbc:h2:tcp://127.0.0.1:1/missing", cipher().encrypt("secret"));

        assertThatThrownBy(() -> inspector.testConnection(dataSource))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
    }

    private DataSource dataSource(String jdbcUrl, String passwordCipher) {
        DataSource dataSource = new DataSource();
        dataSource.setId(1L);
        dataSource.setProjectId(1L);
        dataSource.setName("h2-test");
        dataSource.setDbType("MYSQL");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername("sa");
        dataSource.setPasswordCipher(passwordCipher);
        dataSource.setStatus("ENABLED");
        return dataSource;
    }

    private DataSourcePasswordCipher cipher() {
        AiPythonServiceProperties properties = new AiPythonServiceProperties();
        properties.getSecurity().setDataSourcePasswordKey(KEY);
        return new DataSourcePasswordCipher(properties);
    }
}
