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
    void freshDatabaseMigrationShouldBeSingleDdlOnlyV1() throws IOException {
        List<Path> migrations;
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            migrations = files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrations).extracting(path -> path.getFileName().toString())
                .containsExactly("V1__init_identity.sql");

        String sql = Files.readString(migrations.getFirst()).toUpperCase(Locale.ROOT);
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `IDENTITY_USER`")
                .contains("CREATE TABLE IF NOT EXISTS `TENANT_MEMBER`")
                .contains("CREATE TABLE IF NOT EXISTS `TENANT_MEMBER_ORG`")
                .contains("CREATE TABLE IF NOT EXISTS `IDENTITY_EXTERNAL_BINDING`")
                .contains("`PASSWORD_RESET_REQUIRED`")
                .contains("`LAST_FAILED_LOGIN_AT`");
        assertThat(sql).doesNotContain("INSERT INTO", "UPDATE `", "DELETE FROM", "LOCK TABLES");
    }
}
