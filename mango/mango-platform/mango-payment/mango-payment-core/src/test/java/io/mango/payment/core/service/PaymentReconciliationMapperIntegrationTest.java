package io.mango.payment.core.service;

import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReconciliationMapperIntegrationTest {

    @Test
    @DisplayName("reconciliation mapper should identify local successful payment and refund records missing from bill")
    void selectSuccessfulChannelRecordsMissingInBill_returnsOnlyMissingRows() throws IOException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl("jdbc:h2:mem:payment_reconciliation_mapper_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createTables(jdbcTemplate);
        seedRows(jdbcTemplate);

        SqlSessionFactory sqlSessionFactory = createSqlSessionFactory(dataSource);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PaymentOrderMapper paymentOrderMapper = session.getMapper(PaymentOrderMapper.class);
            List<PaymentOrderEntity> missingPaymentOrders = paymentOrderMapper.selectSuccessfulChannelOrdersMissingInBill(
                    1L,
                    "MANGO_PAY",
                    LocalDate.of(2026, 6, 6),
                    LocalDate.of(2026, 6, 7),
                    List.of("MANGO-PAY-MATCHED"));

            assertThat(missingPaymentOrders)
                    .extracting(PaymentOrderEntity::getPayOrderNo)
                    .containsExactly("PO-MISSING");
            assertThat(missingPaymentOrders.get(0).getAmount()).isEqualTo(9900L);

            PaymentRefundOrderMapper refundOrderMapper = session.getMapper(PaymentRefundOrderMapper.class);
            List<PaymentRefundOrderEntity> missingRefundOrders = refundOrderMapper.selectSuccessfulChannelRefundsMissingInBill(
                    1L,
                    "MANGO_PAY",
                    LocalDate.of(2026, 6, 6),
                    LocalDate.of(2026, 6, 7),
                    List.of("MANGO-REFUND-MATCHED"));

            assertThat(missingRefundOrders)
                    .extracting(PaymentRefundOrderEntity::getRefundOrderNo)
                    .containsExactly("RO-MISSING");
            assertThat(missingRefundOrders.get(0).getRefundAmount()).isEqualTo(3900L);
        }
    }

    private SqlSessionFactory createSqlSessionFactory(SingleConnectionDataSource dataSource) throws IOException {
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(PaymentOrderMapper.class);
        configuration.addMapper(PaymentRefundOrderMapper.class);
        loadMapperXml(configuration, "mapper/payment/PaymentOrderMapper.xml");
        loadMapperXml(configuration, "mapper/payment/PaymentRefundOrderMapper.xml");
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void loadMapperXml(Configuration configuration, String resource) throws IOException {
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
        }
    }

    private void createTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table payment_order (
                  id bigint not null,
                  pay_order_no varchar(64) not null,
                  business_order_id bigint not null,
                  cashier_config_id bigint,
                  channel_id bigint,
                  channel_code varchar(64) not null,
                  channel_merchant_no varchar(64),
                  contract_id bigint,
                  contract_capability_id bigint,
                  route_rule_id bigint,
                  method_id bigint,
                  amount bigint not null,
                  status varchar(32) not null,
                  channel_trade_no varchar(128),
                  success_flag int default 0,
                  pay_time timestamp,
                  expire_time timestamp,
                  tenant_id bigint not null,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table payment_refund_order (
                  id bigint not null,
                  refund_order_no varchar(64) not null,
                  biz_refund_no varchar(64),
                  payment_order_id bigint not null,
                  channel_refund_no varchar(128),
                  refund_amount bigint not null,
                  reason varchar(255),
                  status varchar(32) not null,
                  refund_time timestamp,
                  tenant_id bigint not null,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  primary key (id)
                )
                """);
    }

    private void seedRows(JdbcTemplate jdbcTemplate) {
        LocalDateTime billDay = LocalDateTime.of(2026, 6, 6, 10, 0);
        insertPaymentOrder(jdbcTemplate, 200001L, "PO-MATCHED", "MANGO_PAY", "MANGO-PAY-MATCHED", 1000L, "SUCCESS", 1, billDay);
        insertPaymentOrder(jdbcTemplate, 200002L, "PO-MISSING", "MANGO_PAY", "MANGO-PAY-MISSING", 9900L, "SUCCESS", 1, billDay.plusMinutes(1));
        insertPaymentOrder(jdbcTemplate, 200003L, "PO-FAILED", "MANGO_PAY", "MANGO-PAY-FAILED", 8800L, "FAILED", 0, billDay.plusMinutes(2));
        insertPaymentOrder(jdbcTemplate, 200004L, "PO-OTHER-DAY", "MANGO_PAY", "MANGO-PAY-OTHER-DAY", 7700L, "SUCCESS", 1, billDay.plusDays(1));
        insertPaymentOrder(jdbcTemplate, 200005L, "PO-OTHER-CHANNEL", "ALLINPAY", "ALLINPAY-MISSING", 6600L, "SUCCESS", 1, billDay.plusMinutes(3));

        insertRefundOrder(jdbcTemplate, 300001L, "RO-MATCHED", 200001L, "MANGO-REFUND-MATCHED", 100L, "SUCCESS", billDay.plusMinutes(4));
        insertRefundOrder(jdbcTemplate, 300002L, "RO-MISSING", 200002L, "MANGO-REFUND-MISSING", 3900L, "SUCCESS", billDay.plusMinutes(5));
        insertRefundOrder(jdbcTemplate, 300003L, "RO-FAILED", 200002L, "MANGO-REFUND-FAILED", 500L, "FAILED", billDay.plusMinutes(6));
        insertRefundOrder(jdbcTemplate, 300004L, "RO-OTHER-DAY", 200002L, "MANGO-REFUND-OTHER-DAY", 600L, "SUCCESS", billDay.plusDays(1));
        insertRefundOrder(jdbcTemplate, 300005L, "RO-OTHER-CHANNEL", 200005L, "ALLINPAY-REFUND-MISSING", 700L, "SUCCESS", billDay.plusMinutes(7));
    }

    private void insertPaymentOrder(
            JdbcTemplate jdbcTemplate,
            Long id,
            String payOrderNo,
            String channelCode,
            String channelTradeNo,
            Long amount,
            String status,
            Integer successFlag,
            LocalDateTime payTime) {
        jdbcTemplate.update("""
                insert into payment_order (
                  id, pay_order_no, business_order_id, channel_code, amount, status,
                  channel_trade_no, success_flag, pay_time, tenant_id, created_by, created_at, updated_by, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                payOrderNo,
                100001L,
                channelCode,
                amount,
                status,
                channelTradeNo,
                successFlag,
                payTime,
                1L,
                1001L,
                payTime,
                1001L,
                payTime);
    }

    private void insertRefundOrder(
            JdbcTemplate jdbcTemplate,
            Long id,
            String refundOrderNo,
            Long paymentOrderId,
            String channelRefundNo,
            Long refundAmount,
            String status,
            LocalDateTime refundTime) {
        jdbcTemplate.update("""
                insert into payment_refund_order (
                  id, refund_order_no, payment_order_id, channel_refund_no, refund_amount, status,
                  refund_time, tenant_id, created_by, created_at, updated_by, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                refundOrderNo,
                paymentOrderId,
                channelRefundNo,
                refundAmount,
                status,
                refundTime,
                1L,
                1001L,
                refundTime,
                1001L,
                refundTime);
    }
}
