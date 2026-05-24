package io.mango.payment.starter.service;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.SandboxPaymentCommand;
import io.mango.payment.api.enums.PayBizOrderStatus;
import io.mango.payment.api.enums.PaymentOrderStatus;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.SandboxPaymentNotifyVO;
import io.mango.payment.core.mapper.PayBizOrderMapper;
import io.mango.payment.core.service.IPaymentService;
import io.mango.payment.core.service.impl.PaymentServiceImpl;
import io.mango.payment.starter.PaymentAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        PaymentAutoConfiguration.class,
        SandboxPaymentServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_sandbox;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class SandboxPaymentServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IPaymentService paymentService;

    @Autowired
    private ISandboxPaymentService sandboxPaymentService;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_notify_record");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_refund_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_payment_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_biz_order");
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_biz_order (
                    id BIGINT NOT NULL,
                    app_code VARCHAR(64) NOT NULL,
                    merchant_order_no VARCHAR(128) NOT NULL,
                    subject VARCHAR(128) NOT NULL,
                    amount BIGINT NOT NULL,
                    refunded_amount BIGINT NOT NULL DEFAULT 0,
                    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
                    status VARCHAR(32) NOT NULL,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_pay_biz_order_merchant (tenant_id, app_code, merchant_order_no)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_payment_order (
                    id BIGINT NOT NULL,
                    biz_order_id BIGINT NOT NULL,
                    channel_code VARCHAR(32) NOT NULL,
                    channel_order_no VARCHAR(128),
                    pay_method VARCHAR(32) NOT NULL,
                    idempotency_key VARCHAR(128) NOT NULL,
                    amount BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    material_type VARCHAR(32) NOT NULL,
                    material_content VARCHAR(512) NOT NULL,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_pay_payment_order_idempotency (biz_order_id, idempotency_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_refund_order (
                    id BIGINT NOT NULL,
                    biz_order_id BIGINT NOT NULL,
                    payment_order_id BIGINT NOT NULL,
                    merchant_refund_no VARCHAR(128) NOT NULL,
                    channel_refund_no VARCHAR(128),
                    idempotency_key VARCHAR(128) NOT NULL,
                    refund_amount BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_pay_refund_order_merchant (biz_order_id, merchant_refund_no),
                    UNIQUE KEY uk_mango_pay_refund_order_idempotency (biz_order_id, idempotency_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_notify_record (
                    id BIGINT NOT NULL,
                    notify_event_id VARCHAR(128) NOT NULL,
                    payment_order_id BIGINT NOT NULL,
                    channel_order_no VARCHAR(128) NOT NULL,
                    raw_request VARCHAR(1024),
                    verified TINYINT NOT NULL DEFAULT 0,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_pay_notify_event (notify_event_id)
                )
                """);
    }

    @Test
    void completePayment_sandboxPaymentOrder_marksPaymentSuccess() {
        Long bizOrderId = createOrder("S001", 10_000L);
        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));

        PaymentOrderVO paid = sandboxPaymentService.completePayment(sandboxCommand(payment.getPaymentOrderId(), "sandbox-evt-1"));

        assertThat(paid.getStatus()).isEqualTo(PaymentOrderStatus.SUCCESS);
        assertThat(paymentService.queryBizOrder(queryBizOrderCommand(bizOrderId)).getStatus())
                .isEqualTo(PayBizOrderStatus.PAID);
        assertThat(countRows("mango_pay_notify_record")).isEqualTo(1);
    }

    @Test
    void createPaymentNotify_sandboxPaymentOrder_returnsReusableNotifyCommand() {
        Long bizOrderId = createOrder("S002", 10_000L);
        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));

        SandboxPaymentNotifyVO notify = sandboxPaymentService.createPaymentNotify(
                sandboxCommand(payment.getPaymentOrderId(), "sandbox-evt-2"));

        assertThat(notify.getChannelCode()).isEqualTo("SANDBOX");
        assertThat(notify.getChannelOrderNo()).isEqualTo("SANDBOX-PAY-" + payment.getPaymentOrderId());
        assertThat(paymentService.paymentNotify(notify.getNotifyCommand())).isTrue();
        assertThat(paymentService.queryPaymentOrder(queryPaymentOrderCommand(payment.getPaymentOrderId())).getStatus())
                .isEqualTo(PaymentOrderStatus.SUCCESS);
    }

    private Long createOrder(String merchantOrderNo, long amount) {
        CreatePayBizOrderCommand command = new CreatePayBizOrderCommand();
        command.setAppCode("APP");
        command.setMerchantOrderNo(merchantOrderNo);
        command.setSubject("测试订单");
        command.setAmount(amount);
        command.setCurrency("CNY");
        return paymentService.createBizOrder(command);
    }

    private PayCommand payCommand(Long bizOrderId, String idempotencyKey) {
        PayCommand command = new PayCommand();
        command.setBizOrderId(bizOrderId);
        command.setPayMethod("SANDBOX_QR");
        command.setIdempotencyKey(idempotencyKey);
        return command;
    }

    private QueryPayBizOrderCommand queryBizOrderCommand(Long bizOrderId) {
        QueryPayBizOrderCommand command = new QueryPayBizOrderCommand();
        command.setBizOrderId(bizOrderId);
        return command;
    }

    private io.mango.payment.api.command.QueryPaymentOrderCommand queryPaymentOrderCommand(Long paymentOrderId) {
        io.mango.payment.api.command.QueryPaymentOrderCommand command =
                new io.mango.payment.api.command.QueryPaymentOrderCommand();
        command.setPaymentOrderId(paymentOrderId);
        return command;
    }

    private SandboxPaymentCommand sandboxCommand(Long paymentOrderId, String sandboxEventId) {
        SandboxPaymentCommand command = new SandboxPaymentCommand();
        command.setPaymentOrderId(paymentOrderId);
        command.setSandboxEventId(sandboxEventId);
        return command;
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = PayBizOrderMapper.class)
    static class TestConfig {

        @Bean
        PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
