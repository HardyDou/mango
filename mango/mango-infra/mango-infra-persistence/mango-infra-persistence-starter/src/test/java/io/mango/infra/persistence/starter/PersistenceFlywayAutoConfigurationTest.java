package io.mango.infra.persistence.starter;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlywayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class));

    @Test
    void whenEnabled_shouldCreateFlywayBean() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.user.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx).hasSingleBean(FlywayMigrationInitializer.class);
                });
    }

    @Test
    void flywayBean_shouldUseNoopLocationBecauseInitializerMigratesModules() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    Flyway flyway = ctx.getBean(Flyway.class);
                    ClassicConfiguration config = (ClassicConfiguration) flyway.getConfiguration();
                    List<String> locations = Arrays.stream(config.getLocations())
                            .map(Location::getPath)
                            .collect(Collectors.toList());

                    assertThat(locations).containsExactly("db/migration/_noop");
                });
    }

    @Test
    void disabledModule_shouldNotRunMigration() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=false"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isFalse();
                });
    }

    @Test
    void whenNoModulesSpecified_shouldDiscoverClasspathMigrationModules() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                });
    }

    @Test
    void baselineOnMigrate_shouldBeAcceptedByModuleInitializer() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.baseline-on-migrate=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                });
    }

    @Test
    void customHistoryTable_shouldBeUsedByModuleInitializer() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.history-table=flyway_history_custom_test"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "flyway_history_custom_test")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "flyway_schema_history_persistence_test")).isFalse();
                });
    }

    @Test
    void moduleDatasource_shouldRunMigrationAgainstIndependentDatabase() {
        String moduleUrl = "jdbc:h2:mem:module_flyway_independent;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.datasource.url=" + moduleUrl,
                        "mango.persistence.flyway.modules.persistence-test.datasource.username=sa",
                        "mango.persistence.flyway.modules.persistence-test.datasource.password=",
                        "mango.persistence.flyway.modules.persistence-test.datasource.driver-class-name=org.h2.Driver"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate defaultJdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(defaultJdbcTemplate, "persistence_flyway_user")).isFalse();

                    JdbcTemplate moduleJdbcTemplate = new JdbcTemplate(h2DataSource(moduleUrl));
                    assertThat(tableExists(moduleJdbcTemplate, "persistence_flyway_user")).isTrue();
                    assertThat(tableExists(moduleJdbcTemplate, "flyway_schema_history_persistence_test")).isTrue();
                });
    }

    @Test
    void flywayMigrationInitializer_shouldBeCreated() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(FlywayMigrationInitializer.class);
                });
    }

    @Test
    void customFlyway_shouldNotBeOverridden() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class, CustomFlywayConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx.getBean(Flyway.class)).isSameAs(ctx.getBean("customFlyway"));
                });
    }

    @Test
    void disabled_shouldCreateNonMigratingFlywayToBlockBootDefaultFlow() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=false")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx).hasSingleBean(FlywayMigrationInitializer.class);
                });
    }

    @Configuration
    static class H2DataSourceConfig {
        @Bean
        DataSource dataSource() throws Exception {
            return h2DataSource("jdbc:h2:mem:" + System.nanoTime()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        }
    }

    @Configuration
    static class CustomFlywayConfig {
        @Bean
        Flyway customFlyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:custom/migration")
                    .load();
        }
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public' AND TABLE_NAME = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private static DataSource h2DataSource(String url) {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL(url);
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
