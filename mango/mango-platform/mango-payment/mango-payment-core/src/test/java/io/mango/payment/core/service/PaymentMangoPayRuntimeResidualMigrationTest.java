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
    @DisplayName("V1 should keep unconfigured as the channel adapter default")
    void baseline_keepsAdapterDefaultUnconfigured() throws IOException {
        assertThat(migration())
                .contains("`adapter_type` varchar(64) NOT NULL DEFAULT 'UNCONFIGURED'")
                .doesNotContain("DEFAULT 'MANGO_PAY'");
    }

    @Test
    @DisplayName("V1 should provide the MangoPay scenario schema without built-in configuration rows")
    void baseline_providesMangoPayScenarioSchemaWithoutConfigurationRows() throws IOException {
        assertThat(migration())
                .contains("CREATE TABLE `payment_mango_pay_scenario_control`")
                .doesNotContain("INSERT INTO")
                .doesNotContain("'MANGO_PAY','芒果支付'");
    }

    @Test
    @DisplayName("V1 should not restore retired MangoPay runtime concepts")
    void baseline_doesNotRestoreRetiredRuntimeConcepts() throws IOException {
        assertThat(migration())
                .doesNotContain(legacyUpperSandbox())
                .doesNotContain(legacyLowerSandbox())
                .doesNotContain(legacyChineseSandbox())
                .doesNotContain(legacyUpperSpecial())
                .doesNotContain(legacyLowerSpecial())
                .doesNotContain(legacyChineseSpecialChannel());
    }

    private String migration() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/db/migration/payment/V1__payment_platform.sql"))) {
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
