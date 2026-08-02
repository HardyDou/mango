package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("performance")
@EnabledIfEnvironmentVariable(named = "MANGO_BOOTSTRAP_PERF_DB_URL", matches = "jdbc:mysql:.+")
class PersistenceColdBaselinePerformanceIntegrationTest {

    private static final int MODULE_COUNT = 5;
    private static final int TABLES_PER_MODULE = 75;
    private static final int ROWS_PER_TABLE = 100;
    private static final int PAYLOAD_LENGTH = 400;
    private static final long MINIMUM_SQL_BYTES = 3_030_834L * 5L;
    private static final Duration MAXIMUM_BASELINE_DURATION = Duration.ofSeconds(60);
    private static final String DATABASE_SUFFIX = "_bootstrap_sql_perf";
    private static final List<String> EXISTING_TEST_MODULES = List.of(
            "another-test",
            "business-upgrade",
            "comparison-data",
            "link",
            "payment",
            "persistence-test"
    );

    @Test
    void coldBaselineImportsFiveTimesBaohanSqlScaleWithinOneMinute() throws Exception {
        DataSource dataSource = mysqlDataSource();
        assertEmptyPerformanceDatabase(dataSource);
        List<Path> baselines = generateBaselines();
        long sqlBytes = baselines.stream().mapToLong(this::fileSize).sum();

        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class))
                .withPropertyValues(properties(baselines))
                .withBean(DataSource.class, () -> dataSource);

        long startedAt = System.nanoTime();
        contextRunner.run(context -> {
            PersistenceFlywayBootstrapExecutor.MigrationSummary summary = context
                    .getBean(PersistenceFlywayBootstrapExecutor.class)
                    .applyColdBaseline();
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            assertThat(summary.moduleCount()).isEqualTo(MODULE_COUNT);
            assertThat(summary.migrationCount()).isEqualTo(MODULE_COUNT);
            assertThat(summary.phase()).isEqualTo("COLD_BASELINE");
            assertThat(sqlBytes).isGreaterThanOrEqualTo(MINIMUM_SQL_BYTES);
            assertThat(applicationTableCount(jdbcTemplate)).isEqualTo(MODULE_COUNT * TABLES_PER_MODULE);
            assertThat(rowCount(jdbcTemplate, "perf_m00_t000")).isEqualTo(ROWS_PER_TABLE);
            assertThat(rowCount(jdbcTemplate, "perf_m04_t074")).isEqualTo(ROWS_PER_TABLE);
            assertThat(completedControlCount(jdbcTemplate)).isOne();
            assertThat(completedModuleCount(jdbcTemplate)).isEqualTo(MODULE_COUNT);
            for (int moduleIndex = 0; moduleIndex < MODULE_COUNT; moduleIndex++) {
                assertThat(baselines.get(moduleIndex).getFileName().toString())
                        .isEqualTo("B1__baseline.sql");
                assertThat(completedBaselineHistoryCount(jdbcTemplate, moduleIndex)).isOne();
            }
            assertThat(elapsed).isLessThan(MAXIMUM_BASELINE_DURATION);

            System.out.printf(
                    "MANGO_BOOTSTRAP_SQL_PERF modules=%d tables=%d rows=%d sqlBytes=%d elapsedMs=%d%n",
                    MODULE_COUNT,
                    MODULE_COUNT * TABLES_PER_MODULE,
                    MODULE_COUNT * TABLES_PER_MODULE * ROWS_PER_TABLE,
                    sqlBytes,
                    elapsed.toMillis());
        });
    }

    private static String[] properties(List<Path> baselines) {
        List<String> values = new ArrayList<>();
        values.add("mango.bootstrap.mode=bootstrap");
        values.add("mango.persistence.flyway.upgrade-locations-enabled=false");
        values.add("mango.persistence.flyway.cold-baseline.enabled=true");
        for (String existingModule : EXISTING_TEST_MODULES) {
            values.add("mango.persistence.flyway.modules." + existingModule + ".enabled=false");
            values.add("mango.persistence.flyway.modules." + existingModule
                    + ".skip-reason=not part of the cold baseline performance suite");
        }
        for (int moduleIndex = 0; moduleIndex < baselines.size(); moduleIndex++) {
            String module = moduleCode(moduleIndex);
            values.add("mango.persistence.flyway.modules." + module + ".enabled=true");
            values.add("mango.persistence.flyway.modules." + module + ".baseline.location="
                    + baselines.get(moduleIndex).toUri());
            values.add("mango.persistence.flyway.modules." + module + ".baseline.version=1");
        }
        return values.toArray(String[]::new);
    }

    private static List<Path> generateBaselines() throws IOException {
        Path directory = Files.createTempDirectory("mango-cold-baseline-performance-");
        List<Path> baselines = new ArrayList<>(MODULE_COUNT);
        for (int moduleIndex = 0; moduleIndex < MODULE_COUNT; moduleIndex++) {
            Path moduleDirectory = directory.resolve(moduleCode(moduleIndex));
            Files.createDirectories(moduleDirectory);
            Path baseline = moduleDirectory.resolve("B1__baseline.sql");
            Files.writeString(baseline, baselineSql(moduleIndex));
            baselines.add(baseline);
        }
        return baselines;
    }

    private static String baselineSql(int moduleIndex) {
        StringBuilder sql = new StringBuilder(4_000_000);
        sql.append("-- mango:baseline-idempotent\n");
        for (int tableIndex = 0; tableIndex < TABLES_PER_MODULE; tableIndex++) {
            String tableName = tableName(moduleIndex, tableIndex);
            sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n")
                    .append("  id bigint NOT NULL,\n")
                    .append("  data_code varchar(96) NOT NULL,\n")
                    .append("  payload varchar(512) NOT NULL,\n")
                    .append("  PRIMARY KEY (id),\n")
                    .append("  UNIQUE KEY uk_").append(tableName).append("_code (data_code)\n")
                    .append(");\n")
                    .append("INSERT IGNORE INTO ").append(tableName)
                    .append(" (id, data_code, payload) VALUES\n");
            for (int rowIndex = 0; rowIndex < ROWS_PER_TABLE; rowIndex++) {
                if (rowIndex > 0) {
                    sql.append(",\n");
                }
                String code = "PERF_M%02d_T%03d_R%03d".formatted(moduleIndex, tableIndex, rowIndex);
                String payload = (code + "_").repeat(32);
                sql.append("  (").append(rowIndex + 1).append(", '").append(code).append("', '")
                        .append(payload, 0, PAYLOAD_LENGTH).append("')");
            }
            sql.append(";\n");
        }
        return sql.toString();
    }

    private static DataSource mysqlDataSource() {
        String jdbcUrl = requiredEnvironment("MANGO_BOOTSTRAP_PERF_DB_URL");
        assertDedicatedDatabase(jdbcUrl);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(requiredEnvironment("MANGO_DB_USERNAME"));
        dataSource.setPassword(environment("MANGO_DB_PASSWORD", ""));
        return dataSource;
    }

    private static void assertDedicatedDatabase(String jdbcUrl) {
        String withoutParameters = jdbcUrl.substring(0, jdbcUrl.indexOf('?') >= 0
                ? jdbcUrl.indexOf('?') : jdbcUrl.length());
        String database = withoutParameters.substring(withoutParameters.lastIndexOf('/') + 1);
        assertThat(database)
                .as("MANGO_BOOTSTRAP_PERF_DB_URL must reference a dedicated performance database")
                .endsWith(DATABASE_SUFFIX);
    }

    private static void assertEmptyPerformanceDatabase(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_type = 'BASE TABLE'
                """, Integer.class);
        assertThat(tableCount)
                .as("MANGO_BOOTSTRAP_PERF_DB_URL must reference a dedicated empty database")
                .isZero();
    }

    private static int applicationTableCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_name LIKE 'perf\\_m%'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private static int rowCount(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private static int completedControlCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM mango_cold_baseline_control
                 WHERE status = 'COMPLETED'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private static int completedModuleCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM mango_cold_baseline_module
                 WHERE status = 'COMPLETED' AND baseline_version = '1'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private static int completedBaselineHistoryCount(
            JdbcTemplate jdbcTemplate,
            int moduleIndex) {
        String historyTable = "flyway_schema_history_" + moduleCode(moduleIndex).replace('-', '_');
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + historyTable
                        + " WHERE version = '1' AND `type` = 'BASELINE' AND success = TRUE",
                Integer.class);
        return count == null ? 0 : count;
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Read generated baseline size failed: " + path, exception);
        }
    }

    private static String tableName(int moduleIndex, int tableIndex) {
        return "perf_m%02d_t%03d".formatted(moduleIndex, tableIndex);
    }

    private static String moduleCode(int moduleIndex) {
        return "perf-m%02d".formatted(moduleIndex);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable is required: " + name);
        }
        return value;
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }
}
