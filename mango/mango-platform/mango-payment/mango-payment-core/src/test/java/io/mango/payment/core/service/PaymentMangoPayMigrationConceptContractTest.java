package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMangoPayMigrationConceptContractTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration/payment");

    @Test
    @DisplayName("payment migrations should create mango pay current tables directly")
    void migrations_createMangoPayCurrentTablesDirectly() throws IOException {
        String migrations = migrations();

        assertThat(migrations).contains("CREATE TABLE IF NOT EXISTS `payment_virtual_channel_payment`");
        assertThat(migrations).contains("`virtual_payment_no` varchar(64) NOT NULL COMMENT '内置虚拟通道支付单号'");
        assertThat(migrations).contains("CREATE TABLE IF NOT EXISTS `payment_mango_pay_scenario_control`");
        assertThat(migrations).contains("`channel_code` varchar(32) NOT NULL COMMENT '通道编码，仅支持 MANGO_PAY'");
        assertThat(migrations).contains("`channel_code` = 'MANGO_PAY'");
        assertThat(migrations).contains("`channel_type` = 'BUILTIN_VIRTUAL'");
        assertThat(migrations).contains("`adapter_type` = 'MANGO_PAY'");
    }

    @Test
    @DisplayName("payment migrations should not keep legacy runtime concepts")
    void migrations_doNotKeepLegacyRuntimeConcepts() throws IOException {
        String migrations = migrations();
        String fileNames = fileNames();

        assertNoLegacyConcepts(migrations);
        assertNoLegacyConcepts(fileNames);
    }

    private void assertNoLegacyConcepts(String content) {
        assertThat(content).doesNotContain(legacyUpperSandbox());
        assertThat(content).doesNotContain(legacyLowerSandbox());
        assertThat(content).doesNotContain(legacyChineseSandbox());
        assertThat(content).doesNotContain(legacyUpperSpecial());
        assertThat(content).doesNotContain(legacyLowerSpecial());
        assertThat(content).doesNotContain(legacyChineseSpecialChannel());
        assertThat(content).doesNotContain("payment_" + legacyLowerSandbox());
    }

    private String migrations() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (var files = Files.list(MIGRATION_DIR)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList()) {
                builder.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return builder.toString();
    }

    private String fileNames() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (var files = Files.list(MIGRATION_DIR)) {
            for (Path file : files.sorted().toList()) {
                builder.append(file.getFileName()).append('\n');
            }
        }
        return builder.toString();
    }

    private String legacyUpperSandbox() {
        return new String(new char[] {'S', 'A', 'N', 'D', 'B', 'O', 'X'});
    }

    private String legacyLowerSandbox() {
        return legacyUpperSandbox().toLowerCase();
    }

    private String legacyChineseSandbox() {
        return new String(new char[] {'\u6c99', '\u7bb1'});
    }

    private String legacyUpperSpecial() {
        return new String(new char[] {'S', 'P', 'E', 'C', 'I', 'A', 'L'});
    }

    private String legacyLowerSpecial() {
        return legacyUpperSpecial().toLowerCase();
    }

    private String legacyChineseSpecialChannel() {
        return new String(new char[] {'\u7279', '\u6b8a', '\u901a', '\u9053'});
    }
}
