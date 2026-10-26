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
    private static final String V3_MIGRATION = "db/migration/resource/V3__normalize_resource_registry_unique_keys.sql";
    private static final String V5_MIGRATION = "db/migration/resource/V5__normalize_legacy_resource_sync_modes.sql";
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
    void v3MakesRegistryIdentityUniqueAcrossNullTenantRows() throws IOException {
        String migration = readClasspathResource(V3_MIGRATION);

        assertThat(migration)
                .contains("DROP INDEX `uk_resource_registry_resource_id`")
                .contains("DROP INDEX `uk_resource_registry_type_biz_key`")
                .contains("UNIQUE KEY `uk_resource_registry_resource_id` (`resource_id`)")
                .contains("UNIQUE KEY `uk_resource_registry_type_biz_key` (`resource_type`, `biz_key`)");
    }

    @Test
    void v5NormalizesPersistedLegacySyncModesWithoutChangingTargetData() throws IOException {
        String migration = readClasspathResource(V5_MIGRATION);

        assertThat(migration)
                .contains("SET `sync_mode` = CASE `sync_mode`")
                .contains("WHEN 'AUTO' THEN 'SYNC_ALWAYS'")
                .contains("WHEN 'INIT_ONLY' THEN 'SYNC_ONCE'")
                .contains("WHEN 'MANUAL' THEN 'SYNC_ONCE'")
                .contains("WHEN 'LOCKED' THEN 'SYNC_ONCE'")
                .contains("WHERE `sync_mode` IN ('AUTO', 'INIT_ONLY', 'MANUAL', 'LOCKED')")
                .doesNotContain("resource_version")
                .doesNotContain("source_hash")
                .doesNotContain("target_id")
                .doesNotContain("target_table")
                .doesNotContain("SET `status`")
                .doesNotContain("SET `ownership`");
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
