package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOrderUniqueConstraintMigrationTest {

    @Test
    @DisplayName("payment order migration should enforce channel trade and effective success uniqueness")
    void migration_containsPaymentOrderUniqueConstraints() throws IOException {
        String ddl;
        try (InputStream input = Objects.requireNonNull(
                getClass().getResourceAsStream("/db/migration/payment/V47__payment_order_unique_constraints.sql"))) {
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(ddl).contains("`channel_code` varchar(64) NOT NULL");
        assertThat(ddl).contains("GENERATED ALWAYS AS (CASE WHEN `success_flag` = 1 THEN `business_order_id` ELSE NULL END)");
        assertThat(ddl).contains("UNIQUE KEY `uk_payment_order_channel_trade` (`tenant_id`, `channel_code`, `channel_trade_no`)");
        assertThat(ddl).contains("UNIQUE KEY `uk_payment_order_success_business` (`tenant_id`, `success_business_order_id`)");
    }

    @Test
    @DisplayName("payment order table should reject duplicate channel trades and duplicate effective success")
    void databaseConstraints_rejectDuplicateChannelTradeAndEffectiveSuccess() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl("jdbc:h2:mem:payment_order_unique_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createPaymentOrderTable(jdbcTemplate);

        insertPaymentOrder(jdbcTemplate, 370001L, 360001L, "PO202606060001", "MANGO_PAY", "CH202606060001", 1);

        assertThatThrownBy(() -> insertPaymentOrder(
                jdbcTemplate,
                370002L,
                360002L,
                "PO202606060002",
                "MANGO_PAY",
                "CH202606060001",
                0))
                .isInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> insertPaymentOrder(
                jdbcTemplate,
                370003L,
                360001L,
                "PO202606060003",
                "MANGO_PAY",
                "CH202606060003",
                1))
                .isInstanceOf(DuplicateKeyException.class);

        insertPaymentOrder(jdbcTemplate, 370004L, 360001L, "PO202606060004", "MANGO_PAY", "CH202606060004", 0);
        Long count = jdbcTemplate.queryForObject("select count(1) from payment_order", Long.class);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("payment order table should allow only one concurrent effective success for one business order")
    void databaseConstraints_rejectConcurrentEffectiveSuccess() throws Exception {
        String url = "jdbc:h2:mem:payment_order_concurrent_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createPaymentOrderTable(jdbcTemplate);
        insertPaymentOrder(jdbcTemplate, 370001L, 360001L, "PO202606060001", "MANGO_PAY", "CH202606060001", 0);
        insertPaymentOrder(jdbcTemplate, 370002L, 360001L, "PO202606060002", "MANGO_PAY", "CH202606060002", 0);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> markPaymentSuccess(dataSource, start, 370001L));
            Future<Boolean> second = executor.submit(() -> markPaymentSuccess(dataSource, start, 370002L));

            start.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS) ^ second.get(5, TimeUnit.SECONDS)).isTrue();
            Long effectiveSuccessCount = jdbcTemplate.queryForObject("""
                    select count(1)
                    from payment_order
                    where business_order_id = 360001
                      and success_flag = 1
                    """, Long.class);
            Long nonEffectiveCount = jdbcTemplate.queryForObject("""
                    select count(1)
                    from payment_order
                    where business_order_id = 360001
                      and success_flag = 0
                    """, Long.class);
            assertThat(effectiveSuccessCount).isEqualTo(1L);
            assertThat(nonEffectiveCount).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private void createPaymentOrderTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table payment_order (
                  id bigint not null,
                  business_order_id bigint not null,
                  pay_order_no varchar(64) not null,
                  channel_code varchar(64) not null,
                  channel_trade_no varchar(128),
                  status varchar(32) not null default 'PAYING',
                  success_flag int default 0,
                  success_business_order_id bigint generated always as
                    (case when success_flag = 1 then business_order_id else null end),
                  pay_time timestamp,
                  updated_at timestamp,
                  tenant_id bigint not null,
                  primary key (id),
                  unique key uk_payment_order_channel_trade (tenant_id, channel_code, channel_trade_no),
                  unique key uk_payment_order_success_business (tenant_id, success_business_order_id)
                )
                """);
    }

    private void insertPaymentOrder(
            JdbcTemplate jdbcTemplate,
            Long id,
            Long businessOrderId,
            String payOrderNo,
            String channelCode,
            String channelTradeNo,
            Integer successFlag) {
        jdbcTemplate.update("""
                insert into payment_order (
                  id, business_order_id, pay_order_no, channel_code, channel_trade_no, success_flag, tenant_id
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                businessOrderId,
                payOrderNo,
                channelCode,
                channelTradeNo,
                successFlag,
                1L);
    }

    private Boolean markPaymentSuccess(DriverManagerDataSource dataSource, CountDownLatch start, Long id) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            int updated = new JdbcTemplate(dataSource).update("""
                    update payment_order
                    set status = 'SUCCESS',
                        success_flag = 1,
                        pay_time = current_timestamp,
                        updated_at = current_timestamp
                    where tenant_id = ?
                      and id = ?
                      and status = 'PAYING'
                    """, 1L, id);
            return updated == 1;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
