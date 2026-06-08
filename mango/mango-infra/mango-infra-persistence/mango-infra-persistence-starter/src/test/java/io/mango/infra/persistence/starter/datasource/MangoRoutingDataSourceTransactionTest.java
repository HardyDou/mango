package io.mango.infra.persistence.starter.datasource;

import io.mango.infra.persistence.api.datasource.PersistenceDataSourceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoRoutingDataSourceTransactionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceDataSourceAutoConfiguration.class))
            .withUserConfiguration(TransactionConfig.class)
            .withPropertyValues(
                    "mango.persistence.datasources.primary.primary=true",
                    "mango.persistence.datasources.primary.url=jdbc:h2:mem:primary_tx;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                    "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                    "mango.persistence.datasources.primary.username=sa",
                    "mango.persistence.datasources.primary.password=",
                    "mango.persistence.datasources.job.url=jdbc:h2:mem:job_tx;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                    "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                    "mango.persistence.datasources.job.username=sa",
                    "mango.persistence.datasources.job.password=");

    @Test
    void switchingDatasourceInsideTransaction_shouldFailFast() {
        contextRunner.run(ctx -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
            TransactionTemplate transactionTemplate = new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));

            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("job")) {
                    jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                }
            }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot switch Mango datasource inside one transaction")
                    .hasMessageContaining("current=primary")
                    .hasMessageContaining("target=job");
        });
    }

    @Configuration
    static class TransactionConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
