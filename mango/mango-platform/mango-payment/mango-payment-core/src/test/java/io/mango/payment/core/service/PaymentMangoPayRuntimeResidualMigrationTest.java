package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMangoPayRuntimeResidualMigrationTest {

    @Test
    @DisplayName("mango pay residual migration should keep adapter default unconfigured")
    void migration_keepsAdapterDefaultUnconfigured() throws IOException {
        String migration = migration();

        assertThat(migration).contains("MODIFY COLUMN `adapter_type` varchar(64) NOT NULL DEFAULT 'UNCONFIGURED'");
        assertThat(migration).doesNotContain("DEFAULT 'MANGO_PAY'");
    }

    @Test
    @DisplayName("mango pay residual migration should only maintain mango pay current state")
    void migration_onlyMaintainsMangoPayCurrentState() throws IOException {
        String migration = migration();

        assertThat(migration).contains("WHERE `channel_code` = 'MANGO_PAY'");
        assertThat(migration).contains("WHERE `contract_code` = 'MANGO_PAY_MANGO_TECH'");
        assertThat(migration).contains("`channel_type` = 'BUILTIN_VIRTUAL'");
        assertThat(migration).contains("`adapter_type` = 'MANGO_PAY'");
        assertThat(migration).contains("mangoPayScenario");
        assertThat(migration).contains("mangoPayRefundScenario");
    }

    @Test
    @DisplayName("mango pay residual migration should not keep legacy runtime concepts")
    void migration_doesNotKeepLegacyRuntimeConcepts() throws IOException {
        String migration = migration();

        assertThat(migration).doesNotContain(legacyUpperSandbox());
        assertThat(migration).doesNotContain(legacyLowerSandbox());
        assertThat(migration).doesNotContain(legacyChineseSandbox());
        assertThat(migration).doesNotContain(legacyUpperSpecial());
        assertThat(migration).doesNotContain(legacyLowerSpecial());
        assertThat(migration).doesNotContain(legacyChineseSpecialChannel());
    }

    private String migration() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/db/migration/payment/V63__payment_mango_pay_runtime_residual_guard.sql"))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
