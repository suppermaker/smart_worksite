package com.xd.smartworksite.system.application;

import com.xd.smartworksite.file.infra.MinioStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusApplicationServiceTest {

    @Test
    void versionAndRuntimeReturnObservableRuntimeInformation() {
        SystemStatusApplicationService service = newService(true, true);

        var version = service.version();
        var runtime = service.runtime();

        assertThat(version.getApplicationName()).isEqualTo("smart-worksite");
        assertThat(version.getJavaVersion()).isNotBlank();
        assertThat(runtime.getAvailableProcessors()).isPositive();
        assertThat(runtime.getActiveProfiles()).contains("default");
    }

    @Test
    void dependenciesHealthReportsDownDependencyWithoutThrowing() throws Exception {
        SystemStatusApplicationService service = newService(false, true);

        var health = service.dependenciesHealth();

        assertThat(health.getStatus()).isEqualTo("DEGRADED");
        assertThat(health.getDependencies().get("mysql").getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("mysql").getErrorMessage()).contains("database down");
    }

    private SystemStatusApplicationService newService(boolean mysqlUp, boolean redisUp) {
        MinioStorageProperties minio = new MinioStorageProperties();
        minio.setEndpoint("http://127.0.0.1:1");
        minio.setBucket("test");
        minio.setAccessKey("test");
        minio.setSecretKey("test");
        return new SystemStatusApplicationService(
                new StandardEnvironment(),
                new StubDataSource(mysqlUp),
                redisTemplate(redisUp),
                minio
        );
    }

    private StringRedisTemplate redisTemplate(boolean up) {
        RedisConnection connection = mock(RedisConnection.class);
        when(connection.ping()).thenReturn(up ? "PONG" : "");
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenReturn(connection);
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }

    private static class StubDataSource implements DataSource {
        private final boolean up;

        StubDataSource(boolean up) {
            this.up = up;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (!up) {
                throw new SQLException("database down");
            }
            Connection connection = mock(Connection.class);
            when(connection.isValid(2)).thenReturn(true);
            return connection;
        }

        @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unwrap not supported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
