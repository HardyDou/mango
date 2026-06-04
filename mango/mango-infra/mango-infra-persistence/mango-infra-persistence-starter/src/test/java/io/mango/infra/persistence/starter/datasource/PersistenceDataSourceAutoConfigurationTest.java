package io.mango.infra.persistence.starter.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceDataSourceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceDataSourceAutoConfiguration.class));

    @Test
    void whenNoMangoDatasourceConfigured_shouldNotCreateRegistry() {
        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(PersistenceDataSourceRegistry.class));
    }

    @Test
    void configuredDatasources_shouldCreateRoutingDataSourceAndRegistry() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.datasources.primary.primary=true",
                        "mango.persistence.datasources.primary.url=jdbc:h2:mem:primary_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                        "mango.persistence.datasources.primary.username=sa",
                        "mango.persistence.datasources.primary.password=",
                        "mango.persistence.datasources.job.url=jdbc:h2:mem:job_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                        "mango.persistence.datasources.job.username=sa",
                        "mango.persistence.datasources.job.password=",
                        "mango.persistence.modules.mango-job.datasource=job")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(PersistenceDataSourceRegistry.class);
                    assertThat(ctx).hasSingleBean(DataSource.class);
                    assertThat(ctx.getBean(DataSource.class)).isInstanceOf(MangoRoutingDataSource.class);

                    PersistenceDataSourceRegistry registry = ctx.getBean(PersistenceDataSourceRegistry.class);
                    assertThat(registry.primaryName()).isEqualTo("primary");
                    assertThat(registry.names()).containsExactly("primary", "job");

                    PersistenceModuleDataSourceResolver resolver = ctx.getBean(PersistenceModuleDataSourceResolver.class);
                    assertThat(resolver.resolveDataSource("mango-job")).contains("job");
                });
    }

    @Test
    void routingDataSource_shouldUseContextDatasource() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.datasources.primary.primary=true",
                        "mango.persistence.datasources.primary.url=jdbc:h2:mem:primary_route;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                        "mango.persistence.datasources.primary.username=sa",
                        "mango.persistence.datasources.primary.password=",
                        "mango.persistence.datasources.job.url=jdbc:h2:mem:job_route;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                        "mango.persistence.datasources.job.username=sa",
                        "mango.persistence.datasources.job.password=")
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    jdbcTemplate.execute("CREATE TABLE route_marker (id INT PRIMARY KEY, name VARCHAR(32))");
                    jdbcTemplate.update("INSERT INTO route_marker (id, name) VALUES (1, 'primary')");

                    try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("job")) {
                        jdbcTemplate.execute("CREATE TABLE route_marker (id INT PRIMARY KEY, name VARCHAR(32))");
                        jdbcTemplate.update("INSERT INTO route_marker (id, name) VALUES (1, 'job')");
                    }

                    String primaryName = jdbcTemplate.queryForObject(
                            "SELECT name FROM route_marker WHERE id = 1", String.class);
                    assertThat(primaryName).isEqualTo("primary");

                    try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("job")) {
                        String jobName = jdbcTemplate.queryForObject(
                                "SELECT name FROM route_marker WHERE id = 1", String.class);
                        assertThat(jobName).isEqualTo("job");
                    }
                });
    }
}
