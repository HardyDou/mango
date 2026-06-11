package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouPayMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V90__payment_fuiou_pay_channel_seed.sql");

    @Test
    @DisplayName("migration should seed fuiou channel and fee rate without private key")
    void migration_seedsFuiouFeeRateWithoutPrivateKey() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("0.0020000000")
                .contains("'privateKey', ''")
                .doesNotContain("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc");
    }
}
