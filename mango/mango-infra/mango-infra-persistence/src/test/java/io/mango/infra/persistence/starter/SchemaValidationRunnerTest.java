package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SchemaValidationRunnerTest {

    @Test
    void run_withStandardColumns_passes() throws Exception {
        DataSource dataSource = dataSource("standard");
        execute(dataSource, """
                CREATE TABLE demo_user (
                    id BIGINT PRIMARY KEY,
                    created_by BIGINT,
                    created_at TIMESTAMP,
                    updated_by BIGINT,
                    updated_at TIMESTAMP,
                    tenant_id VARCHAR(64)
                )
                """);

        SchemaValidationRunner runner = new SchemaValidationRunner(dataSource, newProperties(false));

        assertThatNoException().isThrownBy(runner::run);
    }

    @Test
    void run_withMissingColumnsAndWarnMode_doesNotFailStartup() throws Exception {
        DataSource dataSource = dataSource("warn_mode");
        execute(dataSource, """
                CREATE TABLE demo_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(64)
                )
                """);

        SchemaValidationRunner runner = new SchemaValidationRunner(dataSource, newProperties(false));

        assertThatNoException().isThrownBy(runner::run);
    }

    @Test
    void run_withMissingColumnsAndFailFast_failsStartup() throws Exception {
        DataSource dataSource = dataSource("fail_fast");
        execute(dataSource, """
                CREATE TABLE demo_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(64)
                )
                """);

        SchemaValidationRunner runner = new SchemaValidationRunner(dataSource, newProperties(true));

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo_user")
                .hasMessageContaining("created_by")
                .hasMessageContaining("tenant_id");
    }

    private PersistenceProperties.SchemaValidation newProperties(boolean failFast) {
        PersistenceProperties.SchemaValidation properties = new PersistenceProperties.SchemaValidation();
        properties.setFailFast(failFast);
        return properties;
    }

    private DataSource dataSource(String name) {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void execute(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
