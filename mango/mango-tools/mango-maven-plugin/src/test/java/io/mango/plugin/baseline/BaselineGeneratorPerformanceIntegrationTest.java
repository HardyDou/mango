package io.mango.plugin.baseline;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Tag("performance")
@EnabledIfEnvironmentVariable(named = "MANGO_BASELINE_PERF_DB_URL", matches = "jdbc:mysql:.+")
class BaselineGeneratorPerformanceIntegrationTest {

    private static final int MODULE_COUNT = 5;
    private static final int TABLES_PER_MODULE = 75;
    private static final int ROWS_PER_TABLE = 100;
    private static final int PAYLOAD_LENGTH = 400;
    private static final long MINIMUM_MIGRATION_BYTES = 15_000_000L;
    private static final Duration MAXIMUM_DURATION = Duration.ofSeconds(60);

    @TempDir
    Path directory;

    @Test
    void generatesFiveTimesBaohanSqlScaleWithinOneMinute() throws Exception {
        List<String> modules = new ArrayList<>();
        long migrationBytes = 0;
        for (int moduleIndex = 0; moduleIndex < MODULE_COUNT; moduleIndex++) {
            String module = "perf-m%02d".formatted(moduleIndex);
            modules.add(module);
            Path migration = migration(module, migrationSql(moduleIndex));
            migrationBytes += Files.size(migration);
        }
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.copyOf(modules));
        String prefix = "perf_baseline_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        Path output = directory.resolve("target/generated-resources");
        BaselineGenerationSettings settings = new BaselineGenerationSettings(
                requiredEnvironment("MANGO_BASELINE_PERF_DB_URL"),
                environment("MANGO_BASELINE_PERF_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_PERF_DB_PASSWORD", ""),
                prefix,
                MySqlSchemaDefaults.cliStandard(),
                output,
                modules,
                Map.of(),
                false);

        BaselineGenerator.GenerationResult result =
                new BaselineGenerator(settings, catalog, mock(Log.class)).generate();

        assertEquals(MODULE_COUNT, result.moduleCount());
        assertEquals(1, result.datasourceGroupCount());
        assertTrue(migrationBytes >= MINIMUM_MIGRATION_BYTES,
                "migrationBytes=" + migrationBytes);
        assertTrue(result.elapsed().compareTo(MAXIMUM_DURATION) < 0,
                "elapsed=" + result.elapsed());
        long baselineCount;
        try (var paths = Files.walk(output.resolve("db/baseline"))) {
            baselineCount = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("B1__baseline.sql"))
                    .count();
        }
        assertEquals(MODULE_COUNT, baselineCount);
        System.out.printf(
                "MANGO_BASELINE_GENERATOR_PERF modules=%d tables=%d rows=%d migrationBytes=%d elapsedMs=%d%n",
                MODULE_COUNT,
                MODULE_COUNT * TABLES_PER_MODULE,
                MODULE_COUNT * TABLES_PER_MODULE * ROWS_PER_TABLE,
                migrationBytes,
                result.elapsed().toMillis());
    }

    private Path migration(String module, String sql) throws Exception {
        Path path = directory.resolve("app/src/main/resources/db/migration")
                .resolve(module)
                .resolve("V1__init.sql");
        Files.createDirectories(path.getParent());
        Files.writeString(path, sql);
        return path;
    }

    private static String migrationSql(int moduleIndex) {
        StringBuilder sql = new StringBuilder(4_000_000);
        for (int tableIndex = 0; tableIndex < TABLES_PER_MODULE; tableIndex++) {
            String table = "perf_m%02d_t%03d".formatted(moduleIndex, tableIndex);
            sql.append("CREATE TABLE ").append(table).append(" (\n")
                    .append("  id bigint NOT NULL,\n")
                    .append("  data_code varchar(96) NOT NULL,\n")
                    .append("  payload varchar(512) NOT NULL,\n")
                    .append("  PRIMARY KEY (id),\n")
                    .append("  UNIQUE KEY uk_").append(table).append("_code (data_code)\n")
                    .append(");\n")
                    .append("INSERT INTO ").append(table)
                    .append(" (id, data_code, payload) VALUES\n");
            for (int rowIndex = 0; rowIndex < ROWS_PER_TABLE; rowIndex++) {
                if (rowIndex > 0) {
                    sql.append(",\n");
                }
                String code = "PERF_M%02d_T%03d_R%03d".formatted(moduleIndex, tableIndex, rowIndex);
                String payload = (code + "_").repeat(32).substring(0, PAYLOAD_LENGTH);
                sql.append("  (").append(rowIndex + 1).append(", '").append(code)
                        .append("', '").append(payload).append("')");
            }
            sql.append(";\n");
        }
        return sql.toString();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + name);
        }
        return value;
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }
}
