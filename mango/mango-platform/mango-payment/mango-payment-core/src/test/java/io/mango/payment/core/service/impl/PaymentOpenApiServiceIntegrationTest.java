package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOpenApiNonceEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOpenApiNonceMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import io.mango.payment.core.service.IPaymentCashierService;
import io.mango.payment.core.service.IPaymentChannelAdapter;
import io.mango.payment.core.service.PaymentChannelAdapterRegistry;
import io.mango.payment.core.service.PaymentNumberService;
import io.mango.payment.core.service.PaymentOrderStateService;
import io.mango.payment.core.service.PaymentOrderStatusFlowService;
import io.mango.payment.core.service.PaymentRefundApplyService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PaymentOpenApiServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_openapi_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
public class PaymentOpenApiServiceIntegrationTest {

    private static final String APP_ID = "app_openapi";
    private static final String APP_SECRET = "openapi-secret";
    private static final String TENANT_ID = "1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentOpenApiService service;

    @Autowired
    private PaymentBusinessOrderMapper businessOrderMapper;

    @Autowired
    private PaymentOpenApiNonceMapper nonceMapper;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private PaymentRefundOrderMapper refundOrderMapper;

    @Autowired
    private TestPaymentCashierService cashierService;

    @Autowired
    private TestPaymentNumberService numberService;

    @Autowired
    private TestPaymentChannelAdapter channelAdapter;

    @BeforeEach
    void setUp() {
        resetSchema();
        seedBaseData();
        cashierService.clear();
        numberService.nextRefundOrderNo = "RO2026060600000001";
        channelAdapter.refundStatus = "SUCCESS";
        channelAdapter.channelRefundNo = "MRRO2026060600000001";
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createOrderAuthenticatesSignaturePersistsNonceBusinessOrderAndStatusFlowThroughRealMappers() {
        String body = createOrderBody("BIZ_OPENAPI_001", 8800L);
        String timestamp = timestamp();
        String nonce = "nonce-create";

        PaymentOpenBusinessOrderVO result = service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null)).getData();

        PaymentBusinessOrderEntity order = businessOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentBusinessOrderEntity>()
                .eq(PaymentBusinessOrderEntity::getTenantId, 1L)
                .eq(PaymentBusinessOrderEntity::getAppCode, APP_ID)
                .eq(PaymentBusinessOrderEntity::getBizOrderNo, "BIZ_OPENAPI_001"));
        PaymentOpenApiNonceEntity storedNonce = nonceMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentOpenApiNonceEntity>()
                .eq(PaymentOpenApiNonceEntity::getTenantId, 1L)
                .eq(PaymentOpenApiNonceEntity::getAppId, APP_ID)
                .eq(PaymentOpenApiNonceEntity::getNonce, nonce));
        Integer flowCount = jdbcTemplate.queryForObject("""
                select count(1)
                from payment_order_status_flow
                where tenant_id = 1
                  and order_type = 'BUSINESS_ORDER'
                  and order_no = 'BIZ_OPENAPI_001'
                  and trigger_source = 'OPENAPI_CREATE'
                """, Integer.class);

        assertThat(result.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(order).isNotNull();
        assertThat(order.getSubjectId()).isEqualTo(320001L);
        assertThat(order.getAmount()).isEqualTo(8800L);
        assertThat(order.getStatus()).isEqualTo("TO_PAY");
        assertThat(order.getExtendInfo()).contains("businessRefNo");
        assertThat(storedNonce).isNotNull();
        assertThat(storedNonce.getExpireTime()).isAfter(LocalDateTime.now());
        assertThat(flowCount).isEqualTo(1);
    }

    @Test
    void createOrderRejectsReplayNonceThroughRealUniqueConstraint() {
        String body = createOrderBody("BIZ_OPENAPI_002", 8800L);
        String timestamp = timestamp();
        String nonce = "nonce-replay";

        service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null));

        assertThatThrownBy(() -> service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_OPENAPI_NONCE_REPLAY.getMessage());
    }

    @Test
    void createOrderReturnsExistingOrderAndRejectsConflictingIdempotentFieldsThroughRealSelect() {
        insertBusinessOrder("BIZ_OPENAPI_001", 8800L, 0L, "PAYING");
        String sameBody = createOrderBody("BIZ_OPENAPI_001", 8800L);
        String timestamp = timestamp();

        PaymentOpenBusinessOrderVO existing = service.createOrder(openRequest(
                sameBody, APP_ID, TENANT_ID, timestamp, "nonce-idempotent-same",
                signature("POST", "/openapi/pay/orders", sameBody, timestamp, "nonce-idempotent-same"),
                "/openapi/pay/orders", null, null, null, null)).getData();

        String conflictingBody = createOrderBody("BIZ_OPENAPI_001", 9900L);
        assertThat(existing.getId()).isEqualTo(360001L);
        assertThatThrownBy(() -> service.createOrder(openRequest(
                conflictingBody, APP_ID, TENANT_ID, timestamp, "nonce-idempotent-conflict",
                signature("POST", "/openapi/pay/orders", conflictingBody, timestamp, "nonce-idempotent-conflict"),
                "/openapi/pay/orders", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT.getMessage());
    }

    @Test
    void cashierPayDetailAndReceiptUseRealBusinessCashierAndPaymentOrderSql() {
        insertBusinessOrder("BIZ_OPENAPI_001", 8800L, 0L, "PAYING");
        insertSuccessfulPaymentOrder();
        String cashierTimestamp = timestamp();
        String cashierNonce = "nonce-cashier";

        PaymentOpenCashierVO cashier = service.cashier(openRequest(
                "", APP_ID, TENANT_ID, cashierTimestamp, cashierNonce,
                signature("POST", "/openapi/pay/orders/BIZ_OPENAPI_001/cashier", "", cashierTimestamp, cashierNonce),
                "/openapi/pay/orders/BIZ_OPENAPI_001/cashier", null, "BIZ_OPENAPI_001", null, null)).getData();

        String payBody = "{\"methodCode\":\"PERSONAL_WECHAT_QR\"}";
        String payTimestamp = timestamp();
        PaymentOpenPaymentOrderVO payResult = service.pay(openRequest(
                payBody, APP_ID, TENANT_ID, payTimestamp, "nonce-pay",
                signature("POST", "/openapi/pay/orders/BIZ_OPENAPI_001/pay", payBody, payTimestamp, "nonce-pay"),
                "/openapi/pay/orders/BIZ_OPENAPI_001/pay", "192.0.2.20", "BIZ_OPENAPI_001", null, null)).getData();

        String detailTimestamp = timestamp();
        PaymentOpenPaymentOrderVO detail = service.detailPaymentOrder(openRequest(
                null, APP_ID, TENANT_ID, detailTimestamp, "nonce-payment-detail",
                signature("GET", "/openapi/pay/payment-orders/PO202606060001", "", detailTimestamp, "nonce-payment-detail"),
                "/openapi/pay/payment-orders/PO202606060001", null, null, "PO202606060001", null)).getData();

        String receiptTimestamp = timestamp();
        PaymentOpenReceiptVO receipt = service.receipt(openRequest(
                null, APP_ID, TENANT_ID, receiptTimestamp, "nonce-receipt",
                signature("GET", "/openapi/pay/receipts/BIZ_OPENAPI_001", "", receiptTimestamp, "nonce-receipt"),
                "/openapi/pay/receipts/BIZ_OPENAPI_001", null, "BIZ_OPENAPI_001", null, null)).getData();

        assertThat(cashier.getCashierConfigId()).isEqualTo(350001L);
        assertThat(cashier.getCashierUrl()).isEqualTo("/payment/cashier-configs/350001/cashier?businessOrderId=360001");
        assertThat(cashierService.lastCommand.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(cashierService.lastCommand.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(paymentOrderMapper.selectLatestFlowNo(1L, 370001L))
                .isEqualTo("FLOW202606060001");
        assertThat(payResult.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(detail.getStatus()).isEqualTo("SUCCESS");
        assertThat(receipt.getReceiptNo()).isEqualTo("RCPT-BIZ_OPENAPI_001-PO202606060001");
        assertThat(receipt.getFlowNo()).isEqualTo("FLOW202606060001");
    }

    @Test
    void refundCreatesRefundOrderStatusFlowAndApplyResultThroughRealMappers() {
        insertBusinessOrder("BIZ_OPENAPI_001", 8800L, 8800L, "SUCCESS");
        insertSuccessfulPaymentOrder();
        String body = refundBody("RF_OPENAPI_001", 3300L);
        String timestamp = timestamp();

        PaymentOpenRefundOrderVO result = service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, "nonce-refund",
                signature("POST", "/openapi/pay/refunds", body, timestamp, "nonce-refund"),
                "/openapi/pay/refunds", null, null, null, null)).getData();

        PaymentRefundOrderEntity entity = refundOrderMapper.selectById(result.getId());
        Integer flowCount = jdbcTemplate.queryForObject("""
                select count(1)
                from payment_order_status_flow
                where tenant_id = 1
                  and order_type = 'REFUND_ORDER'
                  and order_no = 'RO2026060600000001'
                  and trigger_source = 'OPENAPI_REFUND'
                """, Integer.class);

        assertThat(entity.getBizRefundNo()).isEqualTo("RF_OPENAPI_001");
        assertThat(entity.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(entity.getRefundAmount()).isEqualTo(3300L);
        assertThat(entity.getStatus()).isEqualTo("REFUNDING");
        assertThat(entity.getChannelRefundNo()).isEqualTo("MRRO2026060600000001");
        assertThat(result.getStatus()).isEqualTo("REFUNDING");
        assertThat(channelAdapter.lastRefundCommand.contractId()).isEqualTo(331001L);
        assertThat(flowCount).isEqualTo(1);
    }

    @Test
    void refundRejectsAmountExceededUsingRealSuccessfulOrderLockAndOccupyingRefundSql() {
        insertBusinessOrder("BIZ_OPENAPI_001", 8800L, 8800L, "SUCCESS");
        insertSuccessfulPaymentOrder();
        insertRefundOrder(380001L, "RO2026060600099999", "RF_EXISTING", 7000L, "REFUNDING", "MRO_EXISTING");
        String body = refundBody("RF_OPENAPI_001", 3300L);
        String timestamp = timestamp();

        assertThat(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).isEqualTo(7000L);
        assertThatThrownBy(() -> service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, "nonce-refund-exceeded",
                signature("POST", "/openapi/pay/refunds", body, timestamp, "nonce-refund-exceeded"),
                "/openapi/pay/refunds", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getMessage());
    }

    @Test
    void detailRefundUsesRealRefundJoinAndLatestFlowSql() {
        insertBusinessOrder("BIZ_OPENAPI_001", 8800L, 8800L, "SUCCESS");
        insertSuccessfulPaymentOrder();
        insertRefundOrder(380001L, "RO202606060001", "RF_OPENAPI_001", 3300L, "SUCCESS", "MRRO202606060001");
        jdbcTemplate.update("""
                insert into payment_transaction_flow (id, flow_no, payment_order_id, refund_order_id, tenant_id, created_at)
                values (390002, 'RFLOW202606060001', null, 380001, 1, ?)
                """, LocalDateTime.now());
        String timestamp = timestamp();

        PaymentOpenRefundOrderVO result = service.detailRefund(openRequest(
                null, APP_ID, TENANT_ID, timestamp, "nonce-refund-detail",
                signature("GET", "/openapi/pay/refunds/RF_OPENAPI_001", "", timestamp, "nonce-refund-detail"),
                "/openapi/pay/refunds/RF_OPENAPI_001", null, null, null, "RF_OPENAPI_001")).getData();

        assertThat(result.getRefundOrderNo()).isEqualTo("RO202606060001");
        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("RFLOW202606060001");
    }

    private void resetSchema() {
        jdbcTemplate.execute("""
                create alias if not exists FIND_IN_SET for
                "io.mango.payment.core.service.impl.PaymentOpenApiServiceIntegrationTest.findInSet"
                """);
        jdbcTemplate.execute("drop table if exists payment_order_status_flow");
        jdbcTemplate.execute("drop table if exists payment_transaction_flow");
        jdbcTemplate.execute("drop table if exists payment_refund_order");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_openapi_nonce");
        jdbcTemplate.execute("drop table if exists payment_refund_approval");
        jdbcTemplate.execute("drop table if exists payment_business_order");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_channel");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        jdbcTemplate.execute("drop table if exists payment_application");
        createReferenceTables();
        createOrderTables();
    }

    public static int findInSet(String needle, String csv) {
        if (needle == null || csv == null || csv.isBlank()) {
            return 0;
        }
        String[] values = csv.split(",");
        for (int index = 0; index < values.length; index++) {
            if (needle.equals(values[index].trim())) {
                return index + 1;
            }
        }
        return 0;
    }

    private void createReferenceTables() {
        jdbcTemplate.execute("""
                create table payment_application (
                    id bigint primary key,
                    app_id varchar(128),
                    app_name varchar(128),
                    app_secret varchar(256),
                    secret_configured int,
                    secret_version int,
                    secret_last_reset_time timestamp,
                    sign_algorithm varchar(64),
                    ip_whitelist_enabled int,
                    ip_whitelist varchar(512),
                    payload_encrypt_enabled int,
                    notify_retry_policy varchar(512),
                    demo_app int,
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_enterprise_subject (
                    id bigint primary key,
                    subject_name varchar(128),
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_cashier_config (
                    id bigint primary key,
                    cashier_name varchar(128),
                    application_id bigint,
                    default_cashier int,
                    enterprise_subject_ids varchar(256),
                    method_codes varchar(512),
                    default_method_code varchar(128),
                    method_display_order varchar(512),
                    result_return_url varchar(512),
                    display_config varchar(1024),
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_method (
                    id bigint primary key,
                    method_code varchar(128),
                    method_name varchar(128),
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel (
                    id bigint primary key,
                    channel_code varchar(128),
                    channel_name varchar(128),
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_contract (
                    id bigint primary key,
                    contract_code varchar(128),
                    contract_name varchar(128),
                    subject_id bigint,
                    channel_id bigint,
                    merchant_no varchar(128),
                    config_values_json varchar(1024),
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createOrderTables() {
        jdbcTemplate.execute("""
                create table payment_business_order (
                    id bigint generated by default as identity primary key,
                    biz_order_no varchar(128),
                    app_code varchar(128),
                    title varchar(256),
                    subject_id bigint,
                    amount bigint,
                    paid_amount bigint default 0,
                    refunded_amount bigint default 0,
                    currency varchar(16),
                    status varchar(64),
                    expire_time timestamp,
                    notify_url varchar(512),
                    return_url varchar(512),
                    extend_info varchar(1024),
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_openapi_nonce (
                    id bigint generated by default as identity primary key,
                    app_id varchar(128),
                    nonce varchar(128),
                    expire_time timestamp,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp,
                    unique (tenant_id, app_id, nonce)
                )
                """);
        jdbcTemplate.execute("""
                create table payment_refund_approval (
                    id bigint primary key,
                    payment_order_id bigint,
                    refund_amount bigint,
                    status varchar(64),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_order (
                    id bigint primary key,
                    pay_order_no varchar(128),
                    business_order_id bigint,
                    cashier_config_id bigint,
                    channel_id bigint,
                    channel_code varchar(128),
                    channel_merchant_no varchar(128),
                    contract_id bigint,
                    contract_capability_id bigint,
                    route_rule_id bigint,
                    method_id bigint,
                    amount bigint,
                    status varchar(64),
                    channel_trade_no varchar(128),
                    payment_material_json varchar(2048),
                    success_flag int,
                    pay_time timestamp,
                    expire_time timestamp,
                    tenant_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_refund_order (
                    id bigint generated by default as identity primary key,
                    refund_order_no varchar(128),
                    biz_refund_no varchar(128),
                    payment_order_id bigint,
                    channel_refund_no varchar(128),
                    refund_amount bigint,
                    reason varchar(512),
                    status varchar(64),
                    refund_time timestamp,
                    tenant_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_transaction_flow (
                    id bigint primary key,
                    flow_no varchar(128),
                    payment_order_id bigint,
                    refund_order_id bigint,
                    tenant_id bigint,
                    created_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_order_status_flow (
                    id bigint generated by default as identity primary key,
                    order_type varchar(64),
                    order_id bigint,
                    order_no varchar(128),
                    from_status varchar(64),
                    to_status varchar(64),
                    trigger_source varchar(64),
                    trigger_no varchar(128),
                    operator_id bigint,
                    operator_name varchar(128),
                    happen_time timestamp,
                    remark varchar(512),
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void seedBaseData() {
        jdbcTemplate.update("""
                insert into payment_application (
                    id, app_id, app_name, app_secret, secret_configured, sign_algorithm, status, tenant_id, del_flag
                ) values (310001, ?, 'OpenAPI 应用', 'enc:openapi-secret-ciphertext', 1, 'HMAC_SHA256', 1, 1, 0)
                """, APP_ID);
        jdbcTemplate.update("""
                insert into payment_enterprise_subject (id, subject_name, status, tenant_id, del_flag)
                values (320001, '芒果科技有限公司', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_cashier_config (
                    id, cashier_name, application_id, default_cashier, enterprise_subject_ids, status, tenant_id, del_flag
                ) values (350001, 'OpenAPI 默认收银台', 310001, 1, '320001,320002', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_method (id, method_code, method_name, status, tenant_id, del_flag)
                values (340001, 'PERSONAL_WECHAT_QR', '微信扫码', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_channel (id, channel_code, channel_name, status, tenant_id, del_flag)
                values (330001, 'MANGO_PAY', '芒果支付', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_channel_contract (
                    id, contract_code, contract_name, subject_id, channel_id, merchant_no, config_values_json, status, tenant_id, del_flag
                ) values (
                    331001, 'MANGO_PAY_CONTRACT', '芒果支付签约', 320001, 330001,
                    'MANGO_PAY_MERCHANT_001', '{"mangoPayRefundScenario":"SUCCESS"}', 1, 1, 0
                )
                """);
    }

    private void insertBusinessOrder(String bizOrderNo, Long amount, Long paidAmount, String status) {
        jdbcTemplate.update("""
                insert into payment_business_order (
                    id, biz_order_no, app_code, title, subject_id, amount, paid_amount, refunded_amount,
                    currency, status, expire_time, notify_url, return_url, tenant_id, del_flag
                ) values (
                    360001, ?, ?, '开放接口订单', 320001, ?, ?, 0,
                    'CNY', ?, ?, 'https://business.example.test/payment/notify',
                    'https://business.example.test/payment/result', 1, 0
                )
                """, bizOrderNo, APP_ID, amount, paidAmount, status, LocalDateTime.now().plusMinutes(30));
    }

    private void insertSuccessfulPaymentOrder() {
        jdbcTemplate.update("""
                insert into payment_order (
                    id, pay_order_no, business_order_id, cashier_config_id, channel_id, channel_code,
                    channel_merchant_no, contract_id, contract_capability_id, route_rule_id, method_id,
                    amount, status, channel_trade_no, success_flag, pay_time, expire_time, tenant_id
                ) values (
                    370001, 'PO202606060001', 360001, 350001, 330001, 'MANGO_PAY',
                    'MANGO_PAY_MERCHANT_001', 331001, 333001, 334001, 340001,
                    8800, 'SUCCESS', 'CASHIER-PO202606060001', 1, ?, ?, 1
                )
                """, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(25));
        jdbcTemplate.update("""
                insert into payment_transaction_flow (id, flow_no, payment_order_id, refund_order_id, tenant_id, created_at)
                values (390001, 'FLOW202606060001', 370001, null, 1, ?)
                """, LocalDateTime.now());
    }

    private void insertRefundOrder(Long id, String refundOrderNo, String bizRefundNo, Long amount, String status, String channelRefundNo) {
        jdbcTemplate.update("""
                insert into payment_refund_order (
                    id, refund_order_no, biz_refund_no, payment_order_id, channel_refund_no,
                    refund_amount, reason, status, refund_time, tenant_id
                ) values (?, ?, ?, 370001, ?, ?, '开放接口退款', ?, ?, 1)
                """, id, refundOrderNo, bizRefundNo, channelRefundNo, amount, status,
                "SUCCESS".equals(status) ? LocalDateTime.now() : null);
    }

    private String createOrderBody(String bizOrderNo, Long amount) {
        return "{"
                + "\"tenantId\":1,"
                + "\"appId\":\"" + APP_ID + "\","
                + "\"bizOrderNo\":\"" + bizOrderNo + "\","
                + "\"title\":\"开放接口订单\","
                + "\"amount\":" + amount + ","
                + "\"currency\":\"CNY\","
                + "\"expireMinutes\":30,"
                + "\"notifyUrl\":\"https://business.example.test/payment/notify\","
                + "\"returnUrl\":\"https://business.example.test/payment/result\","
                + "\"extendInfo\":{\"businessRefNo\":\"" + bizOrderNo + "\"}"
                + "}";
    }

    private String refundBody(String bizRefundNo, Long amount) {
        return "{"
                + "\"tenantId\":1,"
                + "\"appId\":\"" + APP_ID + "\","
                + "\"bizOrderNo\":\"BIZ_OPENAPI_001\","
                + "\"bizRefundNo\":\"" + bizRefundNo + "\","
                + "\"refundAmount\":" + amount + ","
                + "\"reason\":\"开放接口退款\""
                + "}";
    }

    private String timestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private PaymentOpenRequestCommand openRequest(
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath,
            String clientIp,
            String bizOrderNo,
            String payOrderNo,
            String bizRefundNo) {
        PaymentOpenRequestCommand command = new PaymentOpenRequestCommand();
        command.setBody(body);
        command.setAppId(appId);
        command.setTenantId(tenantId);
        command.setTimestamp(timestamp);
        command.setNonce(nonce);
        command.setSignature(signature);
        command.setRequestPath(requestPath);
        command.setClientIp(clientIp);
        command.setBizOrderNo(bizOrderNo);
        command.setPayOrderNo(payOrderNo);
        command.setBizRefundNo(bizRefundNo);
        return command;
    }

    private String signature(String method, String path, String body, String timestamp, String nonce) {
        try {
            String canonical = method + "\n" + path + "\n" + sha256Hex(body == null ? "" : body) + "\n" + timestamp + "\n" + nonce;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    @Configuration
    @Import({
            PaymentOpenApiService.class,
            PaymentOrderStateService.class,
            PaymentOrderStatusFlowService.class,
            PaymentRefundApplyService.class,
            PaymentSensitiveValueService.class,
            PaymentChannelAdapterRegistry.class
    })
    @MapperScan("io.mango.payment.core.mapper")
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ICryptoService cryptoService() {
            return new ICryptoService() {
                @Override
                public String encrypt(String plaintext) {
                    return plaintext;
                }

                @Override
                public String encrypt(String plaintext, String iv) {
                    return plaintext;
                }

                @Override
                public String decrypt(String ciphertext) {
                    if ("openapi-secret-ciphertext".equals(ciphertext)) {
                        return APP_SECRET;
                    }
                    return ciphertext;
                }

                @Override
                public String decrypt(String ciphertext, String iv) {
                    return decrypt(ciphertext);
                }
            };
        }

        @Bean
        IPaymentCashierService paymentCashierService() {
            return new TestPaymentCashierService();
        }

        @Bean
        PaymentNumberService paymentNumberService() {
            return new TestPaymentNumberService();
        }

        @Bean
        IPaymentChannelAdapter mangoPayAdapter() {
            return new TestPaymentChannelAdapter();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    static class TestPaymentCashierService implements IPaymentCashierService {

        private PaymentCashierPayCommand lastCommand;

        @Override
        public R<PaymentCashierSessionVO> detailSession(Long cashierConfigId, Long businessOrderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<PaymentCashierPayResultVO> pay(PaymentCashierPayCommand command) {
            this.lastCommand = command;
            PaymentCashierPayResultVO result = new PaymentCashierPayResultVO();
            result.setPayOrderNo("PO202606060001");
            result.setFlowNo("FLOW202606060001");
            result.setStatus("PAYING");
            result.setMethodCode(command.getMethodCode());
            result.setAmount(8800L);
            return R.ok(result);
        }

        @Override
        public R<PaymentCashierPayResultVO> payResult(String payOrderNo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<PaymentCashierPayResultVO> syncPayResult(String payOrderNo) {
            throw new UnsupportedOperationException();
        }

        void clear() {
            this.lastCommand = null;
        }
    }

    static class TestPaymentNumberService extends PaymentNumberService {

        private String nextRefundOrderNo = "RO2026060600000001";

        TestPaymentNumberService() {
            super(null);
        }

        @Override
        public String next(String genKey) {
            if (PAY_REFUND_ORDER_NO.equals(genKey)) {
                return nextRefundOrderNo;
            }
            throw new IllegalArgumentException("unexpected genKey: " + genKey);
        }
    }

    static class TestPaymentChannelAdapter implements IPaymentChannelAdapter {

        private String refundStatus = "SUCCESS";
        private String channelRefundNo = "MRRO2026060600000001";
        private RefundApplyCommand lastRefundCommand;

        @Override
        public String channelCode() {
            return "MANGO_PAY";
        }

        @Override
        public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
            throw new UnsupportedOperationException("payment is covered by cashier service in this OpenAPI test");
        }

        @Override
        public RefundApplyResult applyRefund(RefundApplyCommand command) {
            this.lastRefundCommand = command;
            return new RefundApplyResult("TEST", "SUCCESS", "SYNC", refundStatus, channelRefundNo);
        }

        @Override
        public ChannelBillResult generateBill(ChannelBillCommand command) {
            return new ChannelBillResult(List.<PaymentChannelBillItemRow>of());
        }

        @Override
        public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RefundQueryResult queryRefund(RefundQueryCommand command) {
            throw new UnsupportedOperationException();
        }
    }
}
