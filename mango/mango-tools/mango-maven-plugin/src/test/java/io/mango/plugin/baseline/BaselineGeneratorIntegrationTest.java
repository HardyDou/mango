package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "MANGO_BASELINE_TEST_DB_URL", matches = "jdbc:mysql:.+")
class BaselineGeneratorIntegrationTest {

    @TempDir
    Path directory;

    @Test
    void generatesEquivalentDeterministicBaselinesAcrossDatasourceGroups() throws Exception {
        createCompleteMigrationSet();
        String prefix = uniquePrefix("it_baseline");
        Path output = directory.resolve("target/generated-resources");

        BaselineGenerator.GenerationResult first = generator(prefix, output).generate();
        Map<String, byte[]> firstFiles = resourceFiles(output);
        BaselineGenerator.GenerationResult second = generator(prefix, output).generate();
        Map<String, byte[]> secondFiles = resourceFiles(output);

        assertEquals(3, first.moduleCount());
        assertEquals(2, first.datasourceGroupCount());
        assertEquals(first.generationFingerprint(), second.generationFingerprint());
        assertEquals(firstFiles.keySet(), secondFiles.keySet());
        firstFiles.forEach((path, content) -> assertArrayEquals(content, secondFiles.get(path), path));
        assertTrue(firstFiles.containsKey("db/baseline/alpha/B2__baseline.sql"));
        assertTrue(firstFiles.containsKey("db/baseline/beta/B1__baseline.sql"));
        assertTrue(firstFiles.containsKey("db/baseline/archive/B1__baseline.sql"));
        String alpha = Files.readString(output.resolve("db/baseline/alpha/B2__baseline.sql"));
        assertTrue(alpha.startsWith("-- mango:baseline-idempotent\n"));
        assertTrue(alpha.contains("CREATE TABLE IF NOT EXISTS `alpha_record`"));
        assertTrue(alpha.contains("INSERT IGNORE INTO `alpha_record`"));
        assertTrue(alpha.contains("CREATE OR REPLACE ALGORITHM="));
        String beta = Files.readString(output.resolve("db/baseline/beta/B1__baseline.sql"));
        assertTrue(beta.contains("DROP TRIGGER IF EXISTS `beta_record_bi`"));
        String manifest = Files.readString(output.resolve("META-INF/mango/baseline-manifest.json"));
        assertTrue(manifest.contains("\"generationFingerprint\""));
        assertTrue(manifest.contains("\"datasourceGroup\" : \"archive\""));
        assertFalse(manifest.contains("jdbc:mysql"));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void failedReplayCleansSchemasAndPreservesPreviousOutput() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        migration("alpha", "V2__broken.sql", "THIS IS NOT SQL;");
        String prefix = uniquePrefix("it_failure");
        Path output = directory.resolve("target/generated-resources");
        Path sentinel = output.resolve("db/baseline/existing/B1__baseline.sql");
        Files.createDirectories(sentinel.getParent());
        Files.writeString(sentinel, "existing-output");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(prefix, output, List.of("alpha"), Map.of()).generate());

        assertTrue(exception.getMessage().contains("migration replay"));
        assertEquals("existing-output", Files.readString(sentinel));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void rejectsStoredRoutinesInsteadOfSilentlyOmittingThem() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (id bigint primary key);
                CREATE PROCEDURE alpha_probe() SELECT COUNT(*) FROM alpha_record;
                """);
        String prefix = uniquePrefix("it_routine");
        Path output = directory.resolve("target/generated-resources");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(prefix, output, List.of("alpha"), Map.of()).generate());

        assertTrue(exception.getMessage().contains("stored routines and events are not supported"));
        assertFalse(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void rejectsNondeterministicMigrationData() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (
                  id bigint primary key,
                  generated_code varchar(36) NOT NULL
                );
                INSERT INTO alpha_record (id, generated_code) VALUES (1, UUID());
                """);
        String prefix = uniquePrefix("it_nondeterministic");
        Path output = directory.resolve("target/generated-resources");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(prefix, output, List.of("alpha"), Map.of()).generate());

        assertTrue(exception.getMessage().contains("not deterministic across clean replays"));
        assertFalse(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    private BaselineGenerator generator(String prefix, Path output) throws Exception {
        return generator(
                prefix,
                output,
                List.of("alpha", "beta", "archive"),
                Map.of("archive", "archive"));
    }

    private BaselineGenerator generator(
            String prefix,
            Path output,
            List<String> moduleOrder,
            Map<String, String> groups) throws Exception {
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.copyOf(moduleOrder));
        BaselineGenerationSettings settings = new BaselineGenerationSettings(
                requiredEnvironment("MANGO_BASELINE_TEST_DB_URL"),
                environment("MANGO_BASELINE_TEST_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_TEST_DB_PASSWORD", ""),
                prefix,
                output,
                moduleOrder,
                groups,
                false);
        return new BaselineGenerator(settings, catalog, mock(Log.class));
    }

    private void createCompleteMigrationSet() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (
                  id bigint NOT NULL AUTO_INCREMENT,
                  record_code varchar(64) NOT NULL,
                  payload varbinary(64) NULL,
                  created_at datetime(6) NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_alpha_record_code (record_code)
                );
                INSERT INTO alpha_record (id, record_code, payload, created_at)
                VALUES (1, 'ALPHA-1', X'00FF5C', '2026-07-28 12:13:14.123456');
                CREATE VIEW alpha_record_view AS SELECT id, record_code FROM alpha_record;
                """);
        migration("alpha", "V2__add_note.sql", """
                ALTER TABLE alpha_record ADD note varchar(100) NULL;
                UPDATE alpha_record SET note = 'Mango baseline 数据' WHERE id = 1;
                """);
        migration("beta", "V1__init.sql", """
                CREATE TABLE beta_record (
                  id bigint NOT NULL,
                  record_code varchar(64) NOT NULL,
                  PRIMARY KEY (id)
                );
                INSERT INTO beta_record (id, record_code) VALUES (1, 'beta-1');
                CREATE TRIGGER beta_record_bi BEFORE INSERT ON beta_record
                FOR EACH ROW SET NEW.record_code = UPPER(NEW.record_code);
                """);
        migration("archive", "V1__init.sql", """
                CREATE TABLE archive_record (
                  id bigint NOT NULL,
                  record_code varchar(64) NOT NULL,
                  PRIMARY KEY (id)
                );
                INSERT INTO archive_record (id, record_code) VALUES (1, 'ARCHIVE-1');
                """);
    }

    private void migration(String module, String fileName, String sql) throws IOException {
        Path path = directory.resolve("app/src/main/resources/db/migration")
                .resolve(module)
                .resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sql);
    }

    private int temporaryDatabaseCount(String prefix) throws Exception {
        MySqlJdbcUrl url = MySqlJdbcUrl.parse(requiredEnvironment("MANGO_BASELINE_TEST_DB_URL"));
        try (Connection connection = DriverManager.getConnection(
                url.database("mysql"),
                environment("MANGO_BASELINE_TEST_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_TEST_DB_PASSWORD", ""));
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME LIKE ?")) {
            statement.setString(1, prefix + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static Map<String, byte[]> resourceFiles(Path output) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (var paths = Files.walk(output)) {
            paths.filter(Files::isRegularFile).sorted().forEach(path -> {
                try {
                    files.put(output.relativize(path).toString().replace('\\', '/'),
                            Files.readAllBytes(path));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
        return files;
    }

    private static String uniquePrefix(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
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
