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
import java.sql.Statement;
import java.time.Duration;
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
        assertTrue(alpha.contains("DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"));
        assertTrue(alpha.contains("INSERT IGNORE INTO `alpha_record`"));
        assertTrue(alpha.contains("CREATE OR REPLACE ALGORITHM="));
        String beta = Files.readString(output.resolve("db/baseline/beta/B1__baseline.sql"));
        assertTrue(beta.contains("DROP TRIGGER IF EXISTS `beta_record_bi`"));
        String manifest = Files.readString(output.resolve("META-INF/mango/baseline-manifest.json"));
        assertTrue(manifest.contains("\"generationFingerprint\""));
        assertTrue(manifest.contains("\"targetCharacterSet\" : \"utf8mb4\""));
        assertTrue(manifest.contains("\"targetCollation\" : \"utf8mb4_unicode_ci\""));
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

    @Test
    void allowsRuntimeAuditTimestampsAcrossCleanReplays() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (
                  id bigint primary key,
                  record_code varchar(64) NOT NULL,
                  created_at datetime(6) NOT NULL,
                  updated_at datetime(6) NOT NULL,
                  published_at datetime(6) NOT NULL
                );
                DO SLEEP(0.02);
                INSERT INTO alpha_record (
                  id, record_code, created_at, updated_at, published_at
                ) VALUES (
                  1, 'ALPHA-1', CURRENT_TIMESTAMP(6), NOW(6), CURRENT_TIMESTAMP(6)
                );
                """);
        String prefix = uniquePrefix("it_audit_time");
        Path output = directory.resolve("target/generated-resources");

        generator(prefix, output, List.of("alpha"), Map.of()).generate();

        String baseline = Files.readString(output.resolve("db/baseline/alpha/B1__baseline.sql"));
        assertTrue(baseline.contains("`created_at`, `updated_at`, `published_at`"));
        assertTrue(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void treatsImplicitAndExplicitColumnCharacterSetsAsEquivalent() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE ACT_HI_COMMENT (
                  ID_ varchar(64) not null,
                  TYPE_ varchar(255),
                  TIME_ datetime(3) not null,
                  USER_ID_ varchar(255),
                  TASK_ID_ varchar(64),
                  PROC_INST_ID_ varchar(64),
                  ACTION_ varchar(255),
                  MESSAGE_ varchar(4000),
                  FULL_MSG_ LONGBLOB,
                  primary key (ID_)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_bin;
                INSERT INTO ACT_HI_COMMENT (ID_, TIME_, MESSAGE_)
                VALUES ('comment-1', '2026-08-02 12:00:00.000', 'Mango');
                """);
        String prefix = uniquePrefix("it_charset");
        Path output = directory.resolve("target/generated-resources");

        generator(prefix, output, List.of("alpha"), Map.of()).generate();

        assertTrue(Files.exists(output.resolve("db/baseline/alpha/B1__baseline.sql")));
        assertTrue(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void usesConfiguredSchemaDefaultsInsteadOfMysqlServerDefaults() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (
                  id bigint primary key,
                  record_code varchar(64) NOT NULL,
                  UNIQUE KEY uk_alpha_record_code (record_code)
                );
                """);
        String prefix = uniquePrefix("it_collation");
        Path output = directory.resolve("target/generated-resources");

        generator(
                prefix,
                output,
                List.of("alpha"),
                Map.of(),
                MySqlSchemaDefaults.from("utf8mb4", "utf8mb4_bin")).generate();

        String baseline = Files.readString(output.resolve("db/baseline/alpha/B1__baseline.sql"));
        String manifest = Files.readString(output.resolve("META-INF/mango/baseline-manifest.json"));
        assertTrue(baseline.contains("DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin"));
        assertTrue(manifest.contains("\"targetCollation\" : \"utf8mb4_bin\""));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void rejectsUnsupportedSchemaDefaultsBeforeCreatingTemporaryDatabases() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        String prefix = uniquePrefix("it_bad_collation");
        Path output = directory.resolve("target/generated-resources");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(
                        prefix,
                        output,
                        List.of("alpha"),
                        Map.of(),
                        MySqlSchemaDefaults.from("utf8mb4", "utf8mb4_not_a_collation"))
                        .generate());

        assertTrue(exception.getMessage().contains("MANGO-BASELINE-043"));
        assertFalse(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void rejectsNondeterministicNonAuditTimestampData() throws Exception {
        migration("alpha", "V1__init.sql", """
                CREATE TABLE alpha_record (
                  id bigint primary key,
                  effective_at datetime(6) NOT NULL
                );
                DO SLEEP(0.02);
                INSERT INTO alpha_record (id, effective_at)
                VALUES (1, CURRENT_TIMESTAMP(6));
                """);
        String prefix = uniquePrefix("it_business_time");
        Path output = directory.resolve("target/generated-resources");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(prefix, output, List.of("alpha"), Map.of()).generate());

        assertTrue(exception.getMessage().contains("not deterministic across clean replays"));
        assertFalse(Files.exists(output.resolve("META-INF/mango/baseline-manifest.json")));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void rejectsResourceBaselineAcrossDatasourceGroupsBeforeCreatingSchemas() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        migration("archive", "V1__init.sql", "CREATE TABLE archive_record (id bigint primary key);");
        String prefix = uniquePrefix("it_resource_groups");
        Path output = directory.resolve("target/generated-resources");
        ResourceBaselineExecutionSettings resourceBaseline = new ResourceBaselineExecutionSettings(
                "example.ResourceApplication",
                List.of(),
                directory,
                Duration.ofSeconds(1));

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> generator(
                        prefix,
                        output,
                        List.of("alpha", "archive"),
                        Map.of("archive", "archive"),
                        MySqlSchemaDefaults.cliStandard(),
                        resourceBaseline).generate());

        assertTrue(exception.getMessage().contains("MANGO-BASELINE-048"));
        assertEquals(0, temporaryDatabaseCount(prefix));
    }

    @Test
    void preparesPortableResourceStateWithoutRewritingAuditTimestamps() throws Exception {
        String database = uniquePrefix("it_resource_cleanup");
        MySqlBaselineStore store = baselineStore();
        store.createDatabase("mysql", database, MySqlSchemaDefaults.cliStandard());
        try {
            try (Connection connection = connection(database);
                    Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE resource_registry (
                          id bigint NOT NULL PRIMARY KEY,
                          resource_id varchar(64) NOT NULL UNIQUE,
                          created_at datetime NULL,
                          updated_at datetime NULL,
                          last_sync_time datetime NULL
                        )
                        """);
                statement.execute("""
                        INSERT INTO resource_registry
                          (id, resource_id, created_at, updated_at, last_sync_time)
                        VALUES
                          (900, 'resource-b', '2026-08-29 08:01:02', '2026-08-29 08:03:04', '2026-08-29 08:05:06'),
                          (100, 'resource-a', '2026-08-28 07:01:02', '2026-08-28 07:03:04', '2026-08-28 07:05:06')
                        """);
                for (String table : List.of(
                        "resource_module_receipt", "resource_sync_log", "resource_change_log")) {
                    statement.execute("CREATE TABLE " + table + " (id bigint NOT NULL PRIMARY KEY)");
                    statement.execute("INSERT INTO " + table + " (id) VALUES (1)");
                }
                statement.execute("""
                        CREATE TABLE alpha_record (
                          id bigint NOT NULL PRIMARY KEY,
                          updated_at datetime NOT NULL
                        )
                        """);
                statement.execute("INSERT INTO alpha_record VALUES (1, '2024-02-03 04:05:06')");
                for (String table : List.of(
                        "mango_runtime_instance",
                        "mango_bootstrap_step_execution",
                        "mango_bootstrap_execution",
                        "mango_bootstrap_control")) {
                    statement.execute("CREATE TABLE " + table + " (id bigint NOT NULL PRIMARY KEY)");
                }
            }

            store.preparePortableResourceBaseline(database);

            assertEquals(1, queryLong(database,
                    "SELECT id FROM resource_registry WHERE resource_id = 'resource-a'"));
            assertEquals(2, queryLong(database,
                    "SELECT id FROM resource_registry WHERE resource_id = 'resource-b'"));
            assertEquals("2026-08-28 07:05:06", queryString(database,
                    "SELECT DATE_FORMAT(last_sync_time, '%Y-%m-%d %H:%i:%s') "
                            + "FROM resource_registry WHERE resource_id = 'resource-a'"));
            assertEquals("2024-02-03 04:05:06", queryString(database,
                    "SELECT DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') FROM alpha_record WHERE id = 1"));
            assertEquals(0, queryLong(database, "SELECT COUNT(*) FROM resource_module_receipt"));
            assertEquals(0, queryLong(database, "SELECT COUNT(*) FROM resource_sync_log"));
            assertEquals(0, queryLong(database, "SELECT COUNT(*) FROM resource_change_log"));
            assertEquals(0, queryLong("mysql", """
                    SELECT COUNT(*) FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME LIKE 'mango_bootstrap%%'
                    """.formatted(database)));
            assertEquals(0, queryLong("mysql", """
                    SELECT COUNT(*) FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = 'mango_runtime_instance'
                    """.formatted(database)));
        } finally {
            store.dropDatabase("mysql", database);
        }
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
        return generator(
                prefix,
                output,
                moduleOrder,
                groups,
                MySqlSchemaDefaults.cliStandard());
    }

    private BaselineGenerator generator(
            String prefix,
            Path output,
            List<String> moduleOrder,
            Map<String, String> groups,
            MySqlSchemaDefaults schemaDefaults) throws Exception {
        return generator(prefix, output, moduleOrder, groups, schemaDefaults, null);
    }

    private BaselineGenerator generator(
            String prefix,
            Path output,
            List<String> moduleOrder,
            Map<String, String> groups,
            MySqlSchemaDefaults schemaDefaults,
            ResourceBaselineExecutionSettings resourceBaseline) throws Exception {
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.copyOf(moduleOrder));
        BaselineGenerationSettings settings = new BaselineGenerationSettings(
                requiredEnvironment("MANGO_BASELINE_TEST_DB_URL"),
                environment("MANGO_BASELINE_TEST_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_TEST_DB_PASSWORD", ""),
                prefix,
                schemaDefaults,
                output,
                moduleOrder,
                groups,
                false,
                resourceBaseline);
        return new BaselineGenerator(settings, catalog, mock(Log.class));
    }

    private MySqlBaselineStore baselineStore() throws MojoExecutionException {
        return new MySqlBaselineStore(
                requiredEnvironment("MANGO_BASELINE_TEST_DB_URL"),
                environment("MANGO_BASELINE_TEST_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_TEST_DB_PASSWORD", ""));
    }

    private Connection connection(String database) throws Exception {
        MySqlJdbcUrl url = MySqlJdbcUrl.parse(requiredEnvironment("MANGO_BASELINE_TEST_DB_URL"));
        return DriverManager.getConnection(
                url.database(database),
                environment("MANGO_BASELINE_TEST_DB_USERNAME", "root"),
                environment("MANGO_BASELINE_TEST_DB_PASSWORD", ""));
    }

    private long queryLong(String database, String sql) throws Exception {
        try (Connection connection = connection(database);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String queryString(String database, String sql) throws Exception {
        try (Connection connection = connection(database);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
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
