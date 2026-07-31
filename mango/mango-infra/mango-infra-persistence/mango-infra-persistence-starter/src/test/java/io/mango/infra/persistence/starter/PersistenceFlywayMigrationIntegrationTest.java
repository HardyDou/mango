package io.mango.infra.persistence.starter;

import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleMigrationStatusRegistry;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PersistenceFlywayMigrationIntegrationTest {

    private static final String MODULE_PROPERTY_PREFIX = "mango.persistence.flyway.modules.";

    private static final String TEST_SKIP_REASON = "test fixture is not part of this scenario";

    private static final List<String> TEST_CLASSPATH_MIGRATION_MODULES = List.of(
            "another-test",
            "business-upgrade",
            "comparison-data",
            "link",
            "payment",
            "persistence-test"
    );

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("mango.persistence.flyway.upgrade-locations-enabled=false")
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class));

    @Test
    void bootstrapExecutor_shouldRunRealFlywayMigrationAgainstDatabase() {
        contextRunner
                .withPropertyValues(flywayProperties(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                ))
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    migrate(ctx);

                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM persistence_flyway_user WHERE username = 'migrated'",
                            Integer.class);
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    void diagnosticStateFailureCannotReverseSuccessfulMigration() {
        PersistenceModuleMigrationStatusRegistry registry = mock(PersistenceModuleMigrationStatusRegistry.class);
        doThrow(new IllegalStateException("diagnostic running failed"))
                .when(registry).running(anyString(), anyString());
        doThrow(new IllegalStateException("diagnostic applied failed"))
                .when(registry).applied(anyString(), anyString(), anyString(), anyInt());

        contextRunner
                .withPropertyValues(flywayProperties(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                ))
                .withBean(PersistenceModuleMigrationStatusRegistry.class, () -> registry)
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    migrate(ctx);
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                    verify(registry).unknown(
                            "persistence-test",
                            "flyway_schema_history_persistence_test",
                            "MIGRATION_OBSERVATION_FAILED");
                });
    }

    @Test
    void duplicateVersionsAcrossModules_shouldUseSeparateHistoryTables() {
        contextRunner
                .withPropertyValues(flywayProperties(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.another-test.enabled=true"
                ))
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    migrate(ctx);
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "another_flyway_user")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "flyway_schema_history_persistence_test")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "flyway_schema_history_another_test")).isTrue();
                });
    }

    private static void migrate(org.springframework.context.ApplicationContext context) {
        context.getBean(PersistenceFlywayBootstrapExecutor.class).migrate(BootstrapPhase.EXPAND);
    }

    private static String[] flywayProperties(String... properties) {
        Set<String> configuredModules = Arrays.stream(properties)
                .map(PersistenceFlywayMigrationIntegrationTest::extractConfiguredModule)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> result = new ArrayList<>(Arrays.asList(properties));
        for (String module : TEST_CLASSPATH_MIGRATION_MODULES) {
            if (configuredModules.contains(module)) {
                continue;
            }
            result.add(MODULE_PROPERTY_PREFIX + module + ".enabled=false");
            result.add(MODULE_PROPERTY_PREFIX + module + ".skip-reason=" + TEST_SKIP_REASON);
        }
        return result.toArray(String[]::new);
    }

    private static String extractConfiguredModule(String property) {
        if (!property.startsWith(MODULE_PROPERTY_PREFIX)) {
            return null;
        }
        String tail = property.substring(MODULE_PROPERTY_PREFIX.length());
        int dotIndex = tail.indexOf('.');
        if (dotIndex <= 0) {
            return null;
        }
        return tail.substring(0, dotIndex);
    }

    @Configuration
    static class H2DataSourceConfig {

        @Bean
        DataSource dataSource() {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public' AND TABLE_NAME = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
