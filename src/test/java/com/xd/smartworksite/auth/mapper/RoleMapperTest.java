package com.xd.smartworksite.auth.mapper;

import com.xd.smartworksite.auth.domain.Role;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private RoleMapper roleMapper;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:role_mapper;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS role_permission");
            statement.execute("DROP TABLE IF EXISTS role");
            statement.execute("""
                    CREATE TABLE role (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      role_code VARCHAR(64) NOT NULL,
                      role_name VARCHAR(64) NOT NULL,
                      description VARCHAR(255),
                      status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      created_by BIGINT,
                      updated_by BIGINT,
                      deleted TINYINT NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE role_permission (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      role_id BIGINT NOT NULL,
                      permission_id BIGINT NOT NULL,
                      deleted TINYINT NOT NULL DEFAULT 0
                    )
                    """);
        }

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/auth/RoleMapper.xml"));
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        roleMapper = sqlSessionFactory.openSession(true).getMapper(RoleMapper.class);
    }

    @Test
    void insertWritesGeneratedIdBackToRoleParam() {
        Role role = new Role();
        role.setRoleCode("CUSTOM_ROLE");
        role.setRoleName("Custom Role");
        role.setStatus("ENABLED");

        int inserted = roleMapper.insert(role, 1L);

        assertThat(inserted).isEqualTo(1);
        assertThat(role.getId()).isNotNull();
        assertThat(roleMapper.selectById(role.getId()).getRoleCode()).isEqualTo("CUSTOM_ROLE");
    }
}
