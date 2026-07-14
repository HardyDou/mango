package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouPayMigrationTest {

    private static final Path PAYMENT_BASELINE = Path.of(
            "src/main/resources/db/migration/payment/V1__payment_platform.sql");

    @Test
    @DisplayName("V1 should retain the channel configuration schema without provisioning data or secrets")
    void baseline_retainsChannelConfigurationSchemaWithoutDataOrSecrets() throws Exception {
        String sql = Files.readString(PAYMENT_BASELINE);

        assertThat(sql)
                .contains("CREATE TABLE `payment_channel`")
                .contains("CREATE TABLE `payment_channel_field_template`")
                .contains("CREATE TABLE `payment_channel_contract`")
                .contains("`config_values_json` text")
                .doesNotContain("INSERT INTO")
                .doesNotContain("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc")
                .doesNotContain("vau6p7ldawpezyaugc0kopdrrwm4gkpu")
                .doesNotContain("27.185.20.146")
                .doesNotContain("douxy.inner.yunxinbaokeji.com");
    }
}
