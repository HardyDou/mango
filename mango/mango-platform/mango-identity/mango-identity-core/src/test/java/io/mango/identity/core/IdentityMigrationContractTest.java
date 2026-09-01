package io.mango.identity.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMigrationContractTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src/main/resources/db/migration/identity");

    @Test
    void migrationsShouldContainFreshSchemaAndSupportedUpgrades() throws IOException {
        List<Path> migrations;
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            migrations = files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrations).extracting(path -> path.getFileName().toString())
                .containsExactly(
                        "V1__init_identity.sql",
                        "V2__add_real_name_and_binding_app.sql",
                        "V3__clear_legacy_wecom_display_name_fallback.sql",
                        "V4__add_external_identity_avatar_file.sql",
                        "V5__tenant_member_lifecycle.sql");

        String sql = Files.readString(migrations.getFirst()).toUpperCase(Locale.ROOT);
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `IDENTITY_USER`")
                .contains("CREATE TABLE IF NOT EXISTS `TENANT_MEMBER`")
                .contains("CREATE TABLE IF NOT EXISTS `TENANT_MEMBER_ORG`")
                .contains("CREATE TABLE IF NOT EXISTS `IDENTITY_EXTERNAL_BINDING`")
                .contains("`PASSWORD_RESET_REQUIRED`")
                .contains("`LAST_FAILED_LOGIN_AT`");
        assertThat(sql).doesNotContain("`REAL_NAME`", "`VERIFICATION_STATUS`", "`APP_CODE`",
                "INSERT INTO", "UPDATE `", "DELETE FROM", "LOCK TABLES");

        String upgradeSql = Files.readString(migrations.get(1)).toUpperCase(Locale.ROOT);
        assertThat(upgradeSql).contains("ADD COLUMN `REAL_NAME`")
                .contains("ADD COLUMN `VERIFICATION_STATUS`")
                .contains("ADD COLUMN `APP_CODE`")
                .doesNotContain("INSERT INTO", "DELETE FROM", "LOCK TABLES");

        String lifecycleSql = Files.readString(migrations.get(4)).toUpperCase(Locale.ROOT);
        assertThat(lifecycleSql).contains("CREATE TABLE IF NOT EXISTS `TENANT_MEMBER_LIFECYCLE_LOG`")
                .contains("`EVENT_TYPE` VARCHAR(16) NOT NULL")
                .contains("`OPERATOR_USER_ID` BIGINT")
                .doesNotContain("INSERT INTO", "UPDATE `", "DELETE FROM", "LOCK TABLES");
    }
}
