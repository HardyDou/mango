package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceColdBaselineIntegrationTest {

    private static final List<String> EXISTING_TEST_MODULES = List.of(
            "another-test",
            "business-upgrade",
            "comparison-data",
            "link",
            "payment",
            "persistence-test"
    );

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class))
            .withPropertyValues(
                    "mango.bootstrap.mode=bootstrap",
                    "mango.persistence.flyway.upgrade-locations-enabled=false");

    @Test
    void applyColdBaseline_twoModulesInOneLogicalDatasource_createsModuleHistories() {
        DataSource dataSource = h2DataSource("cold_baseline_single_group");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-beta.enabled=true"))
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> {
                    PersistenceFlywayBootstrapExecutor.MigrationSummary summary = context
                            .getBean(PersistenceFlywayBootstrapExecutor.class)
                            .applyColdBaseline();
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                    assertThat(summary.moduleCount()).isEqualTo(2);
                    assertThat(summary.migrationCount()).isEqualTo(2);
                    assertThat(tableExists(jdbcTemplate, "cold_alpha_record")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "cold_beta_record")).isTrue();
                    assertThat(historyVersion(jdbcTemplate, "flyway_schema_history_cold_alpha"))
                            .isEqualTo("1");
                    assertThat(historyVersion(jdbcTemplate, "flyway_schema_history_cold_beta"))
                            .isEqualTo("2");
                    assertThat(jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM mango_cold_baseline_control WHERE status = 'COMPLETED'",
                            Integer.class)).isEqualTo(1);

                    PersistenceFlywayBootstrapExecutor.MigrationSummary retry = context
                            .getBean(PersistenceFlywayBootstrapExecutor.class)
                            .applyColdBaseline();
                    assertThat(retry.migrationCount()).isZero();
                });
    }

    @Test
    void applyColdBaseline_twoLogicalDatasources_initializesEachDatabase() {
        DataSource defaultDataSource = h2DataSource("cold_baseline_default_unused");
        String alphaUrl = h2Url("cold_baseline_alpha_database");
        String betaUrl = h2Url("cold_baseline_beta_database");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.logical-name=alpha",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.url=" + alphaUrl,
                        "mango.persistence.flyway.modules.cold-alpha.datasource.username=sa",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.password=",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.driver-class-name=org.h2.Driver",
                        "mango.persistence.flyway.modules.cold-beta.enabled=true",
                        "mango.persistence.flyway.modules.cold-beta.datasource.logical-name=beta",
                        "mango.persistence.flyway.modules.cold-beta.datasource.url=" + betaUrl,
                        "mango.persistence.flyway.modules.cold-beta.datasource.username=sa",
                        "mango.persistence.flyway.modules.cold-beta.datasource.password=",
                        "mango.persistence.flyway.modules.cold-beta.datasource.driver-class-name=org.h2.Driver"))
                .withBean(DataSource.class, () -> defaultDataSource)
                .run(context -> {
                    PersistenceFlywayBootstrapExecutor.MigrationSummary summary = context
                            .getBean(PersistenceFlywayBootstrapExecutor.class)
                            .applyColdBaseline();

                    assertThat(summary.migrationCount()).isEqualTo(2);
                    assertThat(tableExists(new JdbcTemplate(h2DataSourceForUrl(alphaUrl)), "cold_alpha_record"))
                            .isTrue();
                    assertThat(tableExists(new JdbcTemplate(h2DataSourceForUrl(betaUrl)), "cold_beta_record"))
                            .isTrue();
                    assertThat(tableExists(new JdbcTemplate(defaultDataSource), "cold_alpha_record"))
                            .isFalse();
                });
    }

    @Test
    void applyColdBaseline_explicitDatasourceWithoutLogicalName_isRejected() {
        String moduleUrl = h2Url("cold_baseline_missing_logical_name");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.url=" + moduleUrl,
                        "mango.persistence.flyway.modules.cold-alpha.datasource.username=sa",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.driver-class-name=org.h2.Driver"))
                .withBean(DataSource.class, () -> h2DataSource("cold_baseline_missing_logical_default"))
                .run(context -> assertThatThrownBy(() -> context
                        .getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline())
                        .hasMessageContaining("explicit datasource requires logical-name")
                        .hasMessageContaining("module=cold-alpha"));
    }

    @Test
    void applyColdBaseline_sameLogicalDatasourceWithDifferentConnections_isRejected() {
        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.logical-name=shared",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.url=" + h2Url("cold_alpha_one"),
                        "mango.persistence.flyway.modules.cold-alpha.datasource.username=sa",
                        "mango.persistence.flyway.modules.cold-alpha.datasource.driver-class-name=org.h2.Driver",
                        "mango.persistence.flyway.modules.cold-beta.enabled=true",
                        "mango.persistence.flyway.modules.cold-beta.datasource.logical-name=shared",
                        "mango.persistence.flyway.modules.cold-beta.datasource.url=" + h2Url("cold_beta_two"),
                        "mango.persistence.flyway.modules.cold-beta.datasource.username=sa",
                        "mango.persistence.flyway.modules.cold-beta.datasource.driver-class-name=org.h2.Driver"))
                .withBean(DataSource.class, () -> h2DataSource("cold_baseline_inconsistent_default"))
                .run(context -> assertThatThrownBy(() -> context
                        .getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline())
                        .hasMessageContaining("inconsistent connection configuration")
                        .hasMessageContaining("datasource=shared"));
    }

    @Test
    void applyColdBaseline_inProgressFingerprintChange_isRejected() throws Exception {
        DataSource dataSource = h2DataSource("cold_baseline_fingerprint_change");
        applySingleAlphaBaseline(dataSource);
        new JdbcTemplate(dataSource).update(
                "UPDATE mango_cold_baseline_control SET status = 'IN_PROGRESS'");
        Path changedBaseline = Files.createTempFile("B3__baseline", ".sql");
        Files.writeString(changedBaseline, """
                -- mango:baseline-idempotent
                CREATE TABLE IF NOT EXISTS cold_alpha_changed (id bigint NOT NULL PRIMARY KEY);
                """);

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-alpha.baseline.location=file:"
                                + changedBaseline.toAbsolutePath(),
                        "mango.persistence.flyway.modules.cold-alpha.baseline.version=3"))
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> assertThatThrownBy(() -> context
                        .getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline())
                        .hasMessageContaining("fingerprint mismatch")
                        .hasMessageContaining("datasource=default"));
    }

    @Test
    void applyColdBaseline_missingModuleStateWithMatchingHistory_repairsWithoutReimport() {
        DataSource dataSource = h2DataSource("cold_baseline_partial_resume");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-beta.enabled=true"))
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> {
                    PersistenceFlywayBootstrapExecutor executor = context
                            .getBean(PersistenceFlywayBootstrapExecutor.class);
                    executor.applyColdBaseline();
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                    jdbcTemplate.update("UPDATE mango_cold_baseline_control SET status = 'IN_PROGRESS'");
                    jdbcTemplate.update("DELETE FROM mango_cold_baseline_module WHERE module_code = 'cold-beta'");

                    PersistenceFlywayBootstrapExecutor.MigrationSummary retry = executor.applyColdBaseline();

                    assertThat(retry.migrationCount()).isZero();
                    assertThat(jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM mango_cold_baseline_module
                             WHERE module_code = 'cold-beta' AND status = 'COMPLETED'
                            """, Integer.class)).isEqualTo(1);
                    assertThat(jdbcTemplate.queryForObject(
                            "SELECT status FROM mango_cold_baseline_control", String.class))
                            .isEqualTo("COMPLETED");
                });
    }

    @Test
    void applyColdBaseline_existingDatabase_skipsSnapshotForNormalUpgrade() {
        DataSource dataSource = h2DataSource("cold_baseline_existing_database");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE existing_application_table (id bigint NOT NULL PRIMARY KEY)");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true"))
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> {
                    PersistenceFlywayBootstrapExecutor.MigrationSummary summary = context
                            .getBean(PersistenceFlywayBootstrapExecutor.class)
                            .applyColdBaseline();

                    assertThat(summary.migrationCount()).isZero();
                    assertThat(tableExists(jdbcTemplate, "cold_alpha_record")).isFalse();
                    assertThat(tableExists(jdbcTemplate, "mango_cold_baseline_control")).isFalse();
                });
    }

    @Test
    void applyColdBaseline_missingIdempotencyMarker_isRejected() throws Exception {
        Path unsafeBaseline = Files.createTempFile("B1__baseline", ".sql");
        Files.writeString(unsafeBaseline,
                "CREATE TABLE unsafe_baseline (id bigint NOT NULL PRIMARY KEY);\n");

        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true",
                        "mango.persistence.flyway.modules.cold-alpha.baseline.location=file:"
                                + unsafeBaseline.toAbsolutePath(),
                        "mango.persistence.flyway.modules.cold-alpha.baseline.version=1"))
                .withBean(DataSource.class, () -> h2DataSource("cold_baseline_unsafe"))
                .run(context -> assertThatThrownBy(() -> context
                        .getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline())
                        .hasMessageContaining("must declare idempotent retry support")
                        .hasMessageContaining("module=cold-alpha"));
    }

    @Test
    void applyColdBaseline_multipleDiscoveredSnapshotsForModule_isRejected() {
        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-duplicate.enabled=true"))
                .withBean(DataSource.class, () -> h2DataSource("cold_baseline_duplicate"))
                .run(context -> assertThatThrownBy(() -> context
                        .getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline())
                        .hasMessageContaining("requires exactly one current SQL per module")
                        .hasMessageContaining("module=cold-duplicate")
                        .hasMessageContaining("count=2"));
    }

    private void applySingleAlphaBaseline(DataSource dataSource) {
        contextRunner
                .withPropertyValues(coldBaselineProperties(
                        "mango.persistence.flyway.modules.cold-alpha.enabled=true"))
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> context.getBean(PersistenceFlywayBootstrapExecutor.class)
                        .applyColdBaseline());
    }

    private static String[] coldBaselineProperties(String... properties) {
        List<String> values = new ArrayList<>(Arrays.asList(properties));
        values.add("mango.persistence.flyway.cold-baseline.enabled=true");
        for (String module : EXISTING_TEST_MODULES) {
            values.add("mango.persistence.flyway.modules." + module + ".enabled=false");
            values.add("mango.persistence.flyway.modules." + module
                    + ".skip-reason=not part of cold baseline scenario");
        }
        return values.toArray(String[]::new);
    }

    private static String historyVersion(JdbcTemplate jdbcTemplate, String table) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM " + table + " WHERE type = 'BASELINE' AND success = TRUE",
                String.class);
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public' AND TABLE_NAME = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private static DataSource h2DataSource(String databaseName) {
        return h2DataSourceForUrl(h2Url(databaseName));
    }

    private static DataSource h2DataSourceForUrl(String url) {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static String h2Url(String databaseName) {
        return "jdbc:h2:mem:" + databaseName
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    }
}
