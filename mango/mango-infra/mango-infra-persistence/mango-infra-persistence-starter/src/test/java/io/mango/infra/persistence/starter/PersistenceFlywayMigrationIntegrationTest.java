package io.mango.infra.persistence.starter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlywayMigrationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class));

    @Test
    void applicationRunner_shouldRunRealFlywayMigrationAgainstDatabase() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);

                    ApplicationRunner runner = ctx.getBean("persistenceFlywayMigrationInitializer", ApplicationRunner.class);
                    runner.run(null);

                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM persistence_flyway_user WHERE username = 'migrated'",
                            Integer.class);
                    assertThat(count).isEqualTo(1);
                });
    }

    @Configuration
    static class H2DataSourceConfig {

        @Bean
        DataSource dataSource() {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:persistence_flyway_migration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }
}
