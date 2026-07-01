package io.mango.infra.persistence.starter;

import com.sun.net.httpserver.HttpServer;
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
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceAutoConfiguration;

import javax.sql.DataSource;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlywayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class));

    private final ApplicationContextRunner multiDataSourceContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceDataSourceAutoConfiguration.class,
                    PersistenceFlywayAutoConfiguration.class));

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
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                )
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
    void outOfOrder_shouldBeAcceptedByModuleInitializer() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.out-of-order=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "persistence_flyway_user")).isTrue();
                });
    }

    @Test
    void mangoPublishedModule_shouldAllowHistoricalLowerVersionMigrationsByDefault() {
        String url = "jdbc:h2:mem:flyway_payment_upgrade_default;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure()
                .dataSource(h2DataSource(url))
                .locations("classpath:db/legacy-migration/payment")
                .table("flyway_schema_history_payment")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.payment.enabled=true"
                )
                .withBean(DataSource.class, () -> h2DataSource(url))
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(h2DataSource(url));
                    assertThat(tableExists(jdbcTemplate, "payment_platform_schema")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "payment_method_contract")).isTrue();
                    assertThat(jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM flyway_schema_history_payment WHERE version IN ('3', '4') AND success = TRUE",
                            Integer.class)).isEqualTo(2);
                });
    }

    @Test
    void explicitOutOfOrderFalse_shouldKeepStrictFlywayOrderingForMangoModule() {
        String url = "jdbc:h2:mem:flyway_payment_upgrade_strict;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure()
                .dataSource(h2DataSource(url))
                .locations("classpath:db/legacy-migration/payment")
                .table("flyway_schema_history_payment")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.payment.enabled=true",
                        "mango.persistence.flyway.modules.payment.out-of-order=false"
                )
                .withBean(DataSource.class, () -> h2DataSource(url))
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("Mango Flyway module migration failed: module=payment")
                            .hasMessageContaining("outOfOrder=false");
                });
    }

    @Test
    void businessModule_shouldKeepStrictFlywayOrderingByDefault() {
        String url = "jdbc:h2:mem:flyway_business_upgrade_strict;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure()
                .dataSource(h2DataSource(url))
                .locations("classpath:db/legacy-migration/business-upgrade")
                .table("flyway_schema_history_business_upgrade")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.business-upgrade.enabled=true"
                )
                .withBean(DataSource.class, () -> h2DataSource(url))
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("Mango Flyway module migration failed: module=business-upgrade")
                            .hasMessageContaining("outOfOrder=false");
                });
    }

    @Test
    void migrationFailure_shouldReportModuleLocationAndHistoryTable() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.history-table=flyway_history_shared_test",
                        "mango.persistence.flyway.modules.another-test.enabled=true",
                        "mango.persistence.flyway.modules.another-test.history-table=flyway_history_shared_test"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("Mango Flyway module migration failed: module=another-test")
                            .hasMessageContaining("historyTable=flyway_history_shared_test")
                            .hasMessageContaining("locations=[classpath:db/migration/another-test]")
                            .hasMessageContaining("outOfOrder=false");
                });
    }

    @Test
    void missingMappedDatasource_shouldReportModuleAndDatasourceContext() {
        String primaryUrl = "jdbc:h2:mem:flyway_primary_missing_mapping;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        multiDataSourceContextRunner
                .withPropertyValues(
                        "mango.persistence.datasources.primary.primary=true",
                        "mango.persistence.datasources.primary.url=" + primaryUrl,
                        "mango.persistence.datasources.primary.username=sa",
                        "mango.persistence.datasources.primary.password=",
                        "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                        "mango.persistence.modules.persistence-test.datasource=missing",
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("Mango Flyway module migration failed: module=persistence-test")
                            .hasMessageContaining("historyTable=<unresolved>")
                            .hasMessageContaining("locations=[classpath:db/migration/persistence-test]")
                            .hasMessageContaining("datasource=missing")
                            .hasMessageContaining("outOfOrder=false");
                });
    }

    @Test
    void customFilesystemLocation_shouldRunExternalMigrationDirectory() throws Exception {
        Path directory = Files.createTempDirectory("mango-flyway-filesystem-");
        Files.writeString(directory.resolve("V9__external_file.sql"), """
                create table external_file_migration (
                    id bigint primary key,
                    data_code varchar(64) not null,
                    data_name varchar(128) not null,
                    data_value int not null
                );

                insert into external_file_migration (id, data_code, data_name, data_value) values
                    (1, 'TC184-FS-001', '文件数据-1', 201),
                    (2, 'TC184-FS-002', '文件数据-2', 202),
                    (3, 'TC184-FS-003', '文件数据-3', 203),
                    (4, 'TC184-FS-004', '文件数据-4', 204),
                    (5, 'TC184-FS-005', '文件数据-5', 205);
                """);

        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.external-file.enabled=true",
                        "mango.persistence.flyway.modules.external-file.locations[0]=filesystem:" + directory.toAbsolutePath()
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "external_file_migration")).isTrue();
                    assertThat(tableExists(jdbcTemplate, "flyway_schema_history_external_file")).isTrue();
                    assertThat(migrationRows(jdbcTemplate, "external_file_migration")).containsExactly(
                            new MigrationDataRow(1L, "TC184-FS-001", "文件数据-1", 201),
                            new MigrationDataRow(2L, "TC184-FS-002", "文件数据-2", 202),
                            new MigrationDataRow(3L, "TC184-FS-003", "文件数据-3", 203),
                            new MigrationDataRow(4L, "TC184-FS-004", "文件数据-4", 204),
                            new MigrationDataRow(5L, "TC184-FS-005", "文件数据-5", 205)
                    );
                });
    }

    @Test
    void classpathAndFilesystemLocations_shouldWriteSameRowsForFiveDatasets() throws Exception {
        AtomicReference<List<MigrationDataRow>> classpathRows = new AtomicReference<>();
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.comparison-data.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "flyway_comparison_data")).isTrue();
                    classpathRows.set(migrationRows(jdbcTemplate, "flyway_comparison_data"));
                    assertThat(classpathRows.get()).containsExactly(
                            new MigrationDataRow(1L, "TC184-OLD-001", "旧模式对比数据-1", 101),
                            new MigrationDataRow(2L, "TC184-OLD-002", "旧模式对比数据-2", 102),
                            new MigrationDataRow(3L, "TC184-OLD-003", "旧模式对比数据-3", 103),
                            new MigrationDataRow(4L, "TC184-OLD-004", "旧模式对比数据-4", 104),
                            new MigrationDataRow(5L, "TC184-OLD-005", "旧模式对比数据-5", 105)
                    );
                });

        Path directory = Files.createTempDirectory("mango-flyway-comparison-");
        Files.writeString(directory.resolve("V1__create_comparison_data.sql"), classpathComparisonSql());

        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.comparison-data.enabled=true",
                        "mango.persistence.flyway.modules.comparison-data.locations[0]=filesystem:"
                                + directory.toAbsolutePath()
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                    assertThat(tableExists(jdbcTemplate, "flyway_comparison_data")).isTrue();
                    assertThat(migrationRows(jdbcTemplate, "flyway_comparison_data"))
                            .containsExactlyElementsOf(classpathRows.get());
                });
    }

    @Test
    void customUrlLocation_shouldDownloadAndRunExternalSqlFile() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] sql = """
                create table external_url_migration (
                    id bigint primary key,
                    data_code varchar(64) not null,
                    data_name varchar(128) not null,
                    data_value int not null
                );

                insert into external_url_migration (id, data_code, data_name, data_value) values
                    (1, 'TC184-URL-001', 'URL数据-1', 301),
                    (2, 'TC184-URL-002', 'URL数据-2', 302),
                    (3, 'TC184-URL-003', 'URL数据-3', 303),
                    (4, 'TC184-URL-004', 'URL数据-4', 304),
                    (5, 'TC184-URL-005', 'URL数据-5', 305);
                """.getBytes(StandardCharsets.UTF_8);
        server.createContext("/V8__external_url.sql", exchange -> {
            exchange.sendResponseHeaders(200, sql.length);
            exchange.getResponseBody().write(sql);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/V8__external_url.sql";
            contextRunner
                    .withPropertyValues(
                            "mango.persistence.flyway.enabled=true",
                            "mango.persistence.flyway.modules.external-url.enabled=true",
                            "mango.persistence.flyway.modules.external-url.locations[0]=" + url
                    )
                    .withUserConfiguration(H2DataSourceConfig.class)
                    .run(ctx -> {
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
                        assertThat(tableExists(jdbcTemplate, "external_url_migration")).isTrue();
                        assertThat(tableExists(jdbcTemplate, "flyway_schema_history_external_url")).isTrue();
                        assertThat(migrationRows(jdbcTemplate, "external_url_migration")).containsExactly(
                                new MigrationDataRow(1L, "TC184-URL-001", "URL数据-1", 301),
                                new MigrationDataRow(2L, "TC184-URL-002", "URL数据-2", 302),
                                new MigrationDataRow(3L, "TC184-URL-003", "URL数据-3", 303),
                                new MigrationDataRow(4L, "TC184-URL-004", "URL数据-4", 304),
                                new MigrationDataRow(5L, "TC184-URL-005", "URL数据-5", 305)
                        );
                    });
        } finally {
            server.stop(0);
        }
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
    void moduleDatasourceMapping_shouldRunMigrationAgainstRegisteredDatasource() {
        String primaryUrl = "jdbc:h2:mem:flyway_primary_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        String jobUrl = "jdbc:h2:mem:flyway_job_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        multiDataSourceContextRunner
                .withPropertyValues(
                        "mango.persistence.datasources.primary.primary=true",
                        "mango.persistence.datasources.primary.url=" + primaryUrl,
                        "mango.persistence.datasources.primary.username=sa",
                        "mango.persistence.datasources.primary.password=",
                        "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                        "mango.persistence.datasources.job.url=" + jobUrl,
                        "mango.persistence.datasources.job.username=sa",
                        "mango.persistence.datasources.job.password=",
                        "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                        "mango.persistence.modules.persistence-test.datasource=job",
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.persistence-test.enabled=true"
                )
                .run(ctx -> {
                    JdbcTemplate primaryJdbcTemplate = new JdbcTemplate(h2DataSource(primaryUrl));
                    assertThat(tableExists(primaryJdbcTemplate, "persistence_flyway_user")).isFalse();

                    JdbcTemplate jobJdbcTemplate = new JdbcTemplate(h2DataSource(jobUrl));
                    assertThat(tableExists(jobJdbcTemplate, "persistence_flyway_user")).isTrue();
                    assertThat(tableExists(jobJdbcTemplate, "flyway_schema_history_persistence_test")).isTrue();
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

    private static List<MigrationDataRow> migrationRows(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.query("""
                select id, data_code, data_name, data_value
                from %s
                order by id
                """.formatted(tableName), (rs, rowNum) -> new MigrationDataRow(
                rs.getLong("id"),
                rs.getString("data_code"),
                rs.getString("data_name"),
                rs.getInt("data_value")
        ));
    }

    private static String classpathComparisonSql() {
        try {
            Path path = Path.of(Objects.requireNonNull(PersistenceFlywayAutoConfigurationTest.class.getResource(
                    "/db/migration/comparison-data/V1__create_comparison_data.sql")).toURI());
            return Files.readString(path);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Invalid comparison migration resource URI", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to read comparison migration resource", ex);
        }
    }

    private static DataSource h2DataSource(String url) {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL(url);
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}

record MigrationDataRow(Long id, String dataCode, String dataName, Integer dataValue) {
}
