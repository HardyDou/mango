package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.enums.PayBizOrderStatus;
import io.mango.payment.api.enums.PaymentOrderStatus;
import io.mango.payment.api.enums.RefundOrderStatus;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.RefundOrderVO;
import io.mango.payment.channel.sandbox.SandboxPaymentChannelProvider;
import io.mango.payment.core.mapper.PayBizOrderMapper;
import io.mango.payment.core.service.IPaymentManagementService;
import io.mango.payment.core.service.IPaymentService;
import io.mango.payment.core.service.IRefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        PaymentServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class PaymentServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IPaymentService paymentService;

    @Autowired
    private IRefundService refundService;

    @Autowired
    private IPaymentManagementService paymentManagementService;

    @Autowired
    private RecordingIdempotent recordingIdempotent;

    @Autowired
    private RecordingLocker recordingLocker;

    @BeforeEach
    void setUp() {
        recordingIdempotent.clear();
        recordingLocker.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_notify_record");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_refund_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_payment_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_biz_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_tenant_cashier");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_method_config");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_manage_item");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_pay_manage_domain");
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
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_manage_domain (
                    id BIGINT NOT NULL,
                    code VARCHAR(64) NOT NULL,
                    title VARCHAR(64) NOT NULL,
                    description VARCHAR(255) NOT NULL,
                    badge VARCHAR(32) NOT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_pay_manage_domain_code (code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_manage_item (
                    id BIGINT NOT NULL,
                    domain VARCHAR(64) NOT NULL,
                    code VARCHAR(64) NOT NULL,
                    name VARCHAR(128) NOT NULL,
                    owner VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    primary_text VARCHAR(255) NOT NULL,
                    secondary_text VARCHAR(255) NOT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_method_config (
                    id BIGINT NOT NULL,
                    method_code VARCHAR(64) NOT NULL,
                    method_name VARCHAR(64) NOT NULL,
                    channel_code VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    single_limit BIGINT NOT NULL DEFAULT 0,
                    sort_order INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mango_pay_tenant_cashier (
                    id BIGINT NOT NULL,
                    tenant_id BIGINT NOT NULL,
                    tenant_name VARCHAR(128) NOT NULL,
                    app_code VARCHAR(64) NOT NULL,
                    cashier_code VARCHAR(64) NOT NULL,
                    cashier_name VARCHAR(128) NOT NULL,
                    enabled_methods VARCHAR(255) NOT NULL,
                    default_method VARCHAR(64) NOT NULL,
                    expire_minutes INT NOT NULL DEFAULT 30,
                    daily_limit BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO mango_pay_manage_domain
                (id, code, title, description, badge, sort_order)
                VALUES (1, 'overview', '总览', '支付平台能力、租户收银台和沙箱链路', '总览', 1),
                       (2, 'cashiers', '收银台配置', '租户收银台样式、可用方式和过期时间', '收银台', 2)
                """);
        jdbcTemplate.update("""
                INSERT INTO mango_pay_manage_item
                (id, domain, code, name, owner, status, primary_text, secondary_text, sort_order, updated_at)
                VALUES (10, 'cashiers', 'CASHIER_STANDARD', '标准收银台', '芒果集团', 'ENABLED', '3 个支付方式', '订单 30 分钟过期', 1, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO mango_pay_method_config
                (id, method_code, method_name, channel_code, status, single_limit, sort_order)
                VALUES (20, 'SANDBOX_QR', '沙箱扫码', 'SANDBOX', 'ENABLED', 5000000, 1),
                       (21, 'SANDBOX_BANK', '沙箱网银', 'SANDBOX', 'DISABLED', 10000000, 2)
                """);
        jdbcTemplate.update("""
                INSERT INTO mango_pay_tenant_cashier
                (id, tenant_id, tenant_name, app_code, cashier_code, cashier_name, enabled_methods, default_method, expire_minutes, daily_limit)
                VALUES (30, 1, '芒果集团', 'mango-admin', 'CASHIER_STANDARD', '标准收银台', 'SANDBOX_QR,SANDBOX_CASHIER', 'SANDBOX_QR', 30, 5000000),
                       (31, 2, '华南事业部', 'mango-south', 'CASHIER_BRANCH', '分支机构收银台', 'SANDBOX_QR', 'SANDBOX_QR', 20, 2000000)
                """);
    }

    @Test
    void pay_validOrder_returnsProcessingPaymentMaterial() {
        Long bizOrderId = createOrder("M001", 10_000L);

        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));

        assertThat(payment.getBizOrderId()).isEqualTo(bizOrderId);
        assertThat(payment.getStatus()).isEqualTo(PaymentOrderStatus.PROCESSING);
        assertThat(payment.getMaterialContent()).startsWith("sandbox://pay/");
        assertThat(paymentService.queryBizOrder(queryBizOrderCommand(bizOrderId)).getStatus()).isEqualTo(PayBizOrderStatus.PAYING);
    }

    @Test
    void pay_sameIdempotencyKey_returnsSamePaymentOrder() {
        Long bizOrderId = createOrder("M002", 10_000L);

        PaymentOrderVO first = paymentService.pay(payCommand(bizOrderId, "pay-1"));
        PaymentOrderVO second = paymentService.pay(payCommand(bizOrderId, "pay-1"));

        assertThat(second.getPaymentOrderId()).isEqualTo(first.getPaymentOrderId());
        assertThat(countRows("mango_pay_payment_order")).isEqualTo(1);
    }

    @Test
    void paymentNotify_validSignature_marksPaymentSuccessAndIsIdempotent() {
        Long bizOrderId = createOrder("M003", 10_000L);
        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));
        String channelOrderNo = SandboxPaymentChannelProvider.sandboxPaymentOrderNo(payment.getPaymentOrderId());
        PaymentNotifyCommand notify = notifyCommand(payment.getPaymentOrderId(), channelOrderNo, "evt-1");

        assertThat(paymentService.paymentNotify(notify)).isTrue();
        assertThat(paymentService.paymentNotify(notify)).isTrue();

        assertThat(paymentService.queryBizOrder(queryBizOrderCommand(bizOrderId)).getStatus()).isEqualTo(PayBizOrderStatus.PAID);
        assertThat(countRows("mango_pay_notify_record")).isEqualTo(1);
    }

    @Test
    void paymentNotify_invalidSignature_recordsAndRejects() {
        Long bizOrderId = createOrder("M004", 10_000L);
        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));
        PaymentNotifyCommand notify = notifyCommand(payment.getPaymentOrderId(),
                SandboxPaymentChannelProvider.sandboxPaymentOrderNo(payment.getPaymentOrderId()), "evt-2");
        notify.setSignature("bad");

        assertThat(paymentService.paymentNotify(notify)).isFalse();

        assertThat(paymentService.queryBizOrder(queryBizOrderCommand(bizOrderId)).getStatus()).isEqualTo(PayBizOrderStatus.PAYING);
        assertThat(countRows("mango_pay_notify_record")).isEqualTo(1);
    }

    @Test
    void closeBizOrder_paidOrder_throwsException() {
        Long bizOrderId = paidOrder("M005", 10_000L);
        ClosePayBizOrderCommand command = new ClosePayBizOrderCommand();
        command.setBizOrderId(bizOrderId);

        assertThatThrownBy(() -> paymentService.closeBizOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已支付业务单不能关闭");
    }

    @Test
    void refund_partialRefund_updatesBizOrderStatus() {
        Long bizOrderId = paidOrder("M006", 10_000L);

        RefundOrderVO refund = refundService.refund(refundCommand(bizOrderId, "R001", 3_000L, "refund-1"));

        assertThat(refund.getStatus()).isEqualTo(RefundOrderStatus.SUCCESS);
        PayBizOrderVO bizOrder = paymentService.queryBizOrder(queryBizOrderCommand(bizOrderId));
        assertThat(bizOrder.getRefundedAmount()).isEqualTo(3_000L);
        assertThat(bizOrder.getStatus()).isEqualTo(PayBizOrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    void refund_exceedPaidAmount_throwsException() {
        Long bizOrderId = paidOrder("M007", 10_000L);

        assertThatThrownBy(() -> refundService.refund(refundCommand(bizOrderId, "R001", 10_001L, "refund-1")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("退款累计金额不能超过已支付金额");
    }

    @Test
    void refund_sameIdempotencyKey_returnsSameRefundOrder() {
        Long bizOrderId = paidOrder("M008", 10_000L);

        RefundOrderVO first = refundService.refund(refundCommand(bizOrderId, "R001", 3_000L, "refund-1"));
        RefundOrderVO second = refundService.refund(refundCommand(bizOrderId, "R002", 3_000L, "refund-1"));

        assertThat(second.getRefundOrderId()).isEqualTo(first.getRefundOrderId());
        assertThat(countRows("mango_pay_refund_order")).isEqualTo(1);
    }

    @Test
    void refund_sameMerchantRefundNo_returnsSameRefundOrder() {
        Long bizOrderId = paidOrder("M010", 10_000L);

        RefundOrderVO first = refundService.refund(refundCommand(bizOrderId, "R001", 3_000L, "refund-1"));
        RefundOrderVO second = refundService.refund(refundCommand(bizOrderId, "R001", 3_000L, "refund-2"));

        assertThat(second.getRefundOrderId()).isEqualTo(first.getRefundOrderId());
        assertThat(countRows("mango_pay_refund_order")).isEqualTo(1);
    }

    @Test
    void pay_refund_notify_useKvIdempotentAndLocker() {
        Long bizOrderId = createOrder("M009", 10_000L);

        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));
        paymentService.paymentNotify(notifyCommand(payment.getPaymentOrderId(),
                SandboxPaymentChannelProvider.sandboxPaymentOrderNo(payment.getPaymentOrderId()), "evt-9"));
        refundService.refund(refundCommand(bizOrderId, "R001", 3_000L, "refund-1"));

        assertThat(recordingIdempotent.keys()).contains(
                "payment:pay:" + bizOrderId + ":pay-1",
                "payment:notify:evt-9",
                "payment:refund:" + bizOrderId + ":refund-1"
        );
        assertThat(recordingLocker.tryLockCount()).isGreaterThanOrEqualTo(3);
        assertThat(recordingLocker.unlockCount()).isEqualTo(recordingLocker.tryLockCount());
    }

    @Test
    void listManagementData_currentTenant_returnsBackendPaymentConfiguration() {
        assertThat(paymentManagementService.listDomains()).extracting("code").contains("overview", "cashiers");
        assertThat(paymentManagementService.listItems("cashiers")).extracting("code").containsExactly("CASHIER_STANDARD");
        assertThat(paymentManagementService.listSandboxMethods()).extracting("code").containsExactly("SANDBOX_QR");
        assertThat(paymentManagementService.listTenantCashiers())
                .hasSize(1)
                .first()
                .satisfies(cashier -> {
                    assertThat(cashier.getTenantId()).isEqualTo(1L);
                    assertThat(cashier.getEnabledMethods()).containsExactly("SANDBOX_QR", "SANDBOX_CASHIER");
                });
    }

    private Long paidOrder(String merchantOrderNo, long amount) {
        Long bizOrderId = createOrder(merchantOrderNo, amount);
        PaymentOrderVO payment = paymentService.pay(payCommand(bizOrderId, "pay-1"));
        paymentService.paymentNotify(notifyCommand(payment.getPaymentOrderId(),
                SandboxPaymentChannelProvider.sandboxPaymentOrderNo(payment.getPaymentOrderId()), merchantOrderNo + "-evt"));
        return bizOrderId;
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

    private PaymentNotifyCommand notifyCommand(Long paymentOrderId, String channelOrderNo, String eventId) {
        PaymentNotifyCommand command = new PaymentNotifyCommand();
        command.setPaymentOrderId(paymentOrderId);
        command.setChannelOrderNo(channelOrderNo);
        command.setNotifyEventId(eventId);
        command.setSignature(SandboxPaymentChannelProvider.signatureOf(paymentOrderId, channelOrderNo, eventId));
        return command;
    }

    private RefundCommand refundCommand(Long bizOrderId, String merchantRefundNo, long amount, String idempotencyKey) {
        RefundCommand command = new RefundCommand();
        command.setBizOrderId(bizOrderId);
        command.setMerchantRefundNo(merchantRefundNo);
        command.setRefundAmount(amount);
        command.setIdempotencyKey(idempotencyKey);
        return command;
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = PayBizOrderMapper.class)
    @ComponentScan(basePackageClasses = {PaymentServiceImpl.class, SandboxPaymentChannelProvider.class})
    static class TestConfig {

        @Bean
        PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        RecordingIdempotent recordingIdempotent() {
            return new RecordingIdempotent();
        }

        @Bean
        RecordingLocker recordingLocker() {
            return new RecordingLocker();
        }
    }

    static class RecordingIdempotent implements IIdempotent {

        private final Set<String> keys = new HashSet<>();

        @Override
        public boolean isDuplicate(String key, long windowSeconds) {
            return keys.contains(key);
        }

        @Override
        public void mark(String key, long windowSeconds) {
            keys.add(key);
        }

        @Override
        public boolean checkAndMark(String key, long windowSeconds) {
            boolean duplicate = keys.contains(key);
            keys.add(key);
            return duplicate;
        }

        void clear() {
            keys.clear();
        }

        Set<String> keys() {
            return keys;
        }
    }

    static class RecordingLocker implements ILocker {

        private int tryLockCount;
        private int unlockCount;

        @Override
        public boolean tryLock(String key, long ttlSeconds) {
            tryLockCount++;
            return true;
        }

        @Override
        public void unlock(String key) {
            unlockCount++;
        }

        void clear() {
            tryLockCount = 0;
            unlockCount = 0;
        }

        int tryLockCount() {
            return tryLockCount;
        }

        int unlockCount() {
            return unlockCount;
        }
    }
}
