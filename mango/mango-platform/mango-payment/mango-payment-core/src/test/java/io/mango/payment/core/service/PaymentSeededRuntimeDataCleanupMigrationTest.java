package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSeededRuntimeDataCleanupMigrationTest {

    @Test
    @DisplayName("seed cleanup migration should remove seeded runtime rows and keep configuration rows")
    void migration_removesSeededRuntimeRowsAndKeepsConfigurationRows() throws IOException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl("jdbc:h2:mem:payment_seed_cleanup_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createTables(jdbcTemplate);
        seedConfigurationRows(jdbcTemplate);
        seedRuntimeRows(jdbcTemplate);

        executeMigration(jdbcTemplate, migration());

        assertThat(count(jdbcTemplate, "payment_application")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_channel")).isEqualTo(1L);
        assertRuntimeRowsOnlyContainLiveRows(jdbcTemplate);
    }

    private void createTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("create table payment_application (id bigint primary key, app_code varchar(64))");
        jdbcTemplate.execute("create table payment_channel (id bigint primary key, channel_code varchar(64))");
        jdbcTemplate.execute("create table payment_operation_audit (id bigint primary key, resource_id varchar(64), operation_action varchar(64))");
        jdbcTemplate.execute("create table payment_settlement_summary (id bigint primary key, channel_code varchar(64), settlement_date date, trade_amount bigint)");
        jdbcTemplate.execute("create table payment_difference (id bigint primary key, difference_no varchar(64), related_order_no varchar(64))");
        jdbcTemplate.execute("create table payment_reconciliation (id bigint primary key, reconciliation_no varchar(64))");
        jdbcTemplate.execute("create table payment_notification_record (id bigint primary key, notification_no varchar(64), related_order_no varchar(64))");
        jdbcTemplate.execute("create table payment_exception_order (id bigint primary key, exception_no varchar(64), related_order_no varchar(64))");
        jdbcTemplate.execute("create table payment_transaction_flow (id bigint primary key, flow_no varchar(64))");
        jdbcTemplate.execute("create table payment_refund_order (id bigint primary key, refund_order_no varchar(64), biz_refund_no varchar(64))");
        jdbcTemplate.execute("create table payment_order (id bigint primary key, pay_order_no varchar(64), channel_trade_no varchar(128))");
        jdbcTemplate.execute("create table payment_business_order (id bigint primary key, biz_order_no varchar(64))");
    }

    private void seedConfigurationRows(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("insert into payment_application (id, app_code) values (?, ?)", 310001L, "ORDER_CENTER");
        jdbcTemplate.update("insert into payment_channel (id, channel_code) values (?, ?)", 330001L, "MANGO_PAY");
    }

    private void seedRuntimeRows(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("insert into payment_operation_audit values (?, ?, ?)", 450001L, "MANGO_PAY", "CREATE_CHANNEL");
        jdbcTemplate.update("insert into payment_operation_audit values (?, ?, ?)", 900001L, "LIVE_RESOURCE", "CREATE_CHANNEL");
        jdbcTemplate.update("insert into payment_settlement_summary values (?, ?, curdate(), ?)", 440001L, "MANGO_PAY", 29800L);
        jdbcTemplate.update("insert into payment_settlement_summary values (?, ?, curdate(), ?)", 900002L, "MANGO_PAY", 1L);
        jdbcTemplate.update("insert into payment_difference values (?, ?, ?)", 430001L, "DF202605250001", "PO202605250003");
        jdbcTemplate.update("insert into payment_difference values (?, ?, ?)", 900003L, "DF-LIVE", "PO-LIVE");
        jdbcTemplate.update("insert into payment_reconciliation values (?, ?)", 420001L, "RC202605250001");
        jdbcTemplate.update("insert into payment_reconciliation values (?, ?)", 900004L, "RC-LIVE");
        jdbcTemplate.update("insert into payment_notification_record values (?, ?, ?)", 410001L, "NT202605250001", "PO202605250001");
        jdbcTemplate.update("insert into payment_notification_record values (?, ?, ?)", 900005L, "NT-LIVE", "PO-LIVE");
        jdbcTemplate.update("insert into payment_exception_order values (?, ?, ?)", 400001L, "EX202605250001", "PO202605250003");
        jdbcTemplate.update("insert into payment_exception_order values (?, ?, ?)", 900006L, "EX-LIVE", "PO-LIVE");
        jdbcTemplate.update("insert into payment_transaction_flow values (?, ?)", 390001L, "FLOW202605250001");
        jdbcTemplate.update("insert into payment_transaction_flow values (?, ?)", 900007L, "FLOW-LIVE");
        jdbcTemplate.update("insert into payment_refund_order values (?, ?, ?)", 380001L, "RO202605250001", "BR202605250001");
        jdbcTemplate.update("insert into payment_refund_order values (?, ?, ?)", 900008L, "RO-LIVE", "BR-LIVE");
        jdbcTemplate.update("insert into payment_order values (?, ?, ?)", 370001L, "PO202605250001", "MANGO_PAY-T202605250001");
        jdbcTemplate.update("insert into payment_order values (?, ?, ?)", 900009L, "PO-LIVE", "MANGO_PAY-LIVE");
        jdbcTemplate.update("insert into payment_business_order values (?, ?)", 360001L, "BO202605250001");
        jdbcTemplate.update("insert into payment_business_order values (?, ?)", 900010L, "BO-LIVE");
    }

    private void assertRuntimeRowsOnlyContainLiveRows(JdbcTemplate jdbcTemplate) {
        assertThat(count(jdbcTemplate, "payment_operation_audit")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_settlement_summary")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_difference")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_reconciliation")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_notification_record")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_exception_order")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_transaction_flow")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_refund_order")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_order")).isEqualTo(1L);
        assertThat(count(jdbcTemplate, "payment_business_order")).isEqualTo(1L);
    }

    private void executeMigration(JdbcTemplate jdbcTemplate, String sql) {
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                jdbcTemplate.execute(trimmed);
            }
        }
    }

    private long count(JdbcTemplate jdbcTemplate, String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(1) from " + tableName, Long.class);
        return count == null ? 0L : count;
    }

    private String migration() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/db/migration/payment/V64__payment_remove_seeded_business_runtime_data.sql"))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
