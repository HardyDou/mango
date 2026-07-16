package io.mango.resource.core.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMigrationContractTest {

    private static final String V1_MIGRATION = "db/migration/resource/V1__init_resource_registry.sql";
    private static final List<String> RESOURCE_TABLES = List.of(
            "resource_registry",
            "resource_sync_log",
            "resource_change_log"
    );
    private static final List<String> TENANT_AUDIT_COLUMNS = List.of(
            "tenant_id",
            "org_id",
            "created_by",
            "created_at",
            "updated_by",
            "updated_at"
    );

    @Test
    void v1ContainsTheCompleteFreshDatabaseSchema() throws IOException {
        String migration = readClasspathResource(V1_MIGRATION);

        for (String table : RESOURCE_TABLES) {
            String createTable = createTableStatement(migration, table);
            assertThat(createTable)
                    .as("fresh database definition for %s", table)
                    .contains(TENANT_AUDIT_COLUMNS.stream()
                            .map(column -> "`" + column + "`")
                            .toArray(String[]::new));
        }
    }

    @Test
    void resourceSchemaDoesNotRelyOnAnAuditColumnPatchMigration() {
        Path migration = Path.of(System.getProperty("basedir"), "src/main/resources/db/migration/resource",
                "V2__add_resource_registry_audit_user_columns.sql");

        assertThat(Files.exists(migration)).isFalse();
    }

    private String readClasspathResource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String createTableStatement(String migration, String table) {
        String marker = "CREATE TABLE IF NOT EXISTS `" + table + "`";
        int start = migration.indexOf(marker);
        assertThat(start).as("CREATE TABLE statement for %s", table).isGreaterThanOrEqualTo(0);
        int end = migration.indexOf(';', start);
        assertThat(end).as("terminating semicolon for %s", table).isGreaterThan(start);
        return migration.substring(start, end);
    }
}
