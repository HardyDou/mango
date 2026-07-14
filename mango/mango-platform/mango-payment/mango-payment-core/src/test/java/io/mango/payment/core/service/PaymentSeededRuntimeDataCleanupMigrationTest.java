package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSeededRuntimeDataCleanupMigrationTest {

    private static final Pattern DML = Pattern.compile(
            "(?im)^\\s*(insert|update|delete|replace|merge)\\s+(into\\s+|from\\s+)?");

    private static final List<String> RUNTIME_TABLES = List.of(
            "payment_business_order",
            "payment_order",
            "payment_refund_order",
            "payment_transaction_flow",
            "payment_exception_order",
            "payment_notification_record",
            "payment_reconciliation",
            "payment_difference",
            "payment_settlement_summary",
            "payment_operation_audit",
            "payment_channel_bill_batch",
            "payment_order_status_flow",
            "payment_offline_refund_process");

    @Test
    @DisplayName("V1 should contain DDL only and provision no configuration, runtime rows, or merchant secrets")
    void baseline_containsOnlyDdl() throws IOException {
        String sql = migration();

        assertThat(sql)
                .contains("CREATE TABLE `payment_application`")
                .contains("CREATE TABLE `payment_channel`")
                .contains("CREATE TABLE `payment_method_category`")
                .contains("CREATE TABLE `payment_method_route_rule`")
                .doesNotContain("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc")
                .doesNotContain("vau6p7ldawpezyaugc0kopdrrwm4gkpu");
        assertThat(DML.matcher(sql).find()).isFalse();
        RUNTIME_TABLES.forEach(table -> assertThat(sql)
                .as(table)
                .doesNotContain("INSERT INTO `" + table + "`"));
    }

    private String migration() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/db/migration/payment/V1__payment_platform.sql"))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
