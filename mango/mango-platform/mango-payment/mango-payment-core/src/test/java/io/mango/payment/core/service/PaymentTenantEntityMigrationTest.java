package io.mango.payment.core.service;

import io.mango.infra.persistence.api.entity.TenantEntity;
import io.mango.payment.core.entity.PaymentBaseEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTenantEntityMigrationTest {

    private static final String MIGRATION = "/db/migration/payment/V1__payment_platform.sql";

    private static final Set<String> PAYMENT_TABLES = Set.of(
            "payment_application", "payment_enterprise_subject", "payment_channel", "payment_method",
            "payment_cashier_config", "payment_business_order", "payment_order", "payment_refund_order",
            "payment_transaction_flow", "payment_exception_order", "payment_notification_record",
            "payment_reconciliation", "payment_difference", "payment_settlement_summary",
            "payment_operation_audit", "payment_virtual_channel_payment", "payment_channel_contract",
            "payment_channel_capability", "payment_channel_contract_capability", "payment_method_route_rule",
            "payment_method_route_rule_item", "payment_method_category", "payment_channel_bill_detail",
            "payment_openapi_nonce", "payment_channel_query_record", "payment_refund_query_record",
            "payment_mango_pay_scenario_control", "payment_tenant", "payment_subject_bank_account",
            "payment_channel_field_template", "payment_channel_contract_value", "payment_channel_bill_batch",
            "payment_risk_rule", "payment_order_status_flow", "payment_channel_certificate_rotation_record",
            "payment_refund_approval", "payment_offline_collection", "payment_offline_refund_process",
            "payment_offline_collection_voucher", "payment_offline_bank_statement_batch",
            "payment_offline_bank_statement_item", "payment_offline_collection_match",
            "payment_channel_bill_source", "payment_channel_bill_fetch_batch");

    @Test
    void paymentBaseEntityUsesThePlatformTenantContract() throws NoSuchMethodException {
        assertThat(PaymentBaseEntity.class).isAssignableTo(TenantEntity.class);
        assertThat(PaymentBaseEntity.class.getMethod("getTenantId").getReturnType()).isEqualTo(String.class);
        assertThat(PaymentBaseEntity.class.getMethod("getOrgId").getReturnType()).isEqualTo(Long.class);
    }

    @Test
    void migrationAlignsEveryPaymentEntityTableWithThePlatformTenantContract() throws IOException {
        String ddl;
        try (var input = getClass().getResourceAsStream(MIGRATION)) {
            assertThat(input).as(MIGRATION).isNotNull();
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(ddl.lines().filter(line -> line.startsWith("CREATE TABLE `payment_")).count())
                .isEqualTo(PAYMENT_TABLES.size());
        PAYMENT_TABLES.forEach(table -> assertThat(tableDdl(ddl, table))
                .as(table)
                .contains("`tenant_id` varchar(64)")
                .contains("`org_id` bigint DEFAULT NULL"));
    }

    private String tableDdl(String ddl, String table) {
        String marker = "CREATE TABLE `" + table + "`";
        int start = ddl.indexOf(marker);
        assertThat(start).as(marker).isGreaterThanOrEqualTo(0);
        int end = ddl.indexOf(";", start);
        assertThat(end).as(table + " statement end").isGreaterThan(start);
        return ddl.substring(start, end + 1);
    }
}
