package io.mango.infra.db.starter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MangoFlywayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MangoFlywayAutoConfiguration.class));

    @Test
    void whenEnabled_shouldCreateFlywayBean() {
        contextRunner
                .withPropertyValues(
                        "mango.flyway.enabled=true",
                        "mango.flyway.modules.user.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx).hasSingleBean(org.springframework.boot.ApplicationRunner.class);
                });
    }

    @Test
    void applicationRunner_shouldBeCreated() {
        contextRunner
                .withPropertyValues("mango.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(org.springframework.boot.ApplicationRunner.class);
                });
    }

    @Configuration
    static class H2DataSourceConfig {
        @Bean
        DataSource dataSource() throws Exception {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }
}
