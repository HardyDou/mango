package io.mango.system.core.resource;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMigrationContractTest {

    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration/system");
    private static final Pattern DATA_OR_PATCH_STATEMENT = Pattern.compile(
            "(?im)^\\s*(insert\\s+into|update\\s+[^`\\s]|delete\\s+from|alter\\s+table)\\b");

    @Test
    void freshDatabaseUsesSingleDdlOnlyV1() throws Exception {
        try (var files = Files.list(MIGRATION_ROOT)) {
            assertThat(files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList())
                    .containsExactly("V1__init_system.sql");
        }
        String sql = Files.readString(MIGRATION_ROOT.resolve("V1__init_system.sql"), StandardCharsets.UTF_8);

        assertThat(DATA_OR_PATCH_STATEMENT.matcher(sql).find()).isFalse();
        assertThat(sql.toLowerCase()).containsOnlyOnce("create table if not exists `sys_tenant`");
        assertThat(count(sql.toLowerCase(), "create table if not exists")).isEqualTo(9);
    }

    @Test
    void everySystemTableDeclaresCanonicalTenantAndAuditColumns() throws Exception {
        String sql = Files.readString(MIGRATION_ROOT.resolve("V1__init_system.sql"), StandardCharsets.UTF_8);

        for (String table : new String[]{
                "sys_dict_type", "sys_dict_data", "sys_config", "sys_login_log", "sys_operation_log",
                "sys_tenant", "sys_area", "sys_i18n", "sys_personal_config"}) {
            String ddl = tableDdl(sql, table);
            assertThat(ddl).as(table)
                    .contains("`tenant_id`", "`org_id`", "`created_by`", "`created_at`", "`updated_by`", "`updated_at`")
                    .doesNotContain("`create_by`", "`create_time`", "`update_by`", "`update_time`");
        }
    }

    private String tableDdl(String sql, String table) {
        int start = sql.indexOf("CREATE TABLE IF NOT EXISTS `" + table + "`");
        assertThat(start).as(table).isGreaterThanOrEqualTo(0);
        int end = sql.indexOf(";", start);
        assertThat(end).as(table).isGreaterThan(start);
        return sql.substring(start, end);
    }

    private int count(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
