package io.mango.auth.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMigrationContractTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration/auth");

    @Test
    void migrationCreatesTenantAppProviderScopedEncryptedConfiguration() throws IOException {
        List<Path> migrations;
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            migrations = files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrations).extracting(path -> path.getFileName().toString())
                .containsExactly("V1__init_auth.sql");

        String sql = Files.readString(migrations.getFirst()).toUpperCase(Locale.ROOT);
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `AUTH_PROVIDER_CONFIG`")
                .contains("`TENANT_ID`")
                .contains("`APP_CODE`")
                .contains("`PROVIDER`")
                .contains("`SECRET_CIPHERTEXT`")
                .contains("CREATE UNIQUE INDEX `UK_AUTH_PROVIDER_CONFIG_SCOPE`")
                .doesNotContain("`SECRET` VARCHAR", "INSERT INTO", "DELETE FROM", "LOCK TABLES");
    }
}
