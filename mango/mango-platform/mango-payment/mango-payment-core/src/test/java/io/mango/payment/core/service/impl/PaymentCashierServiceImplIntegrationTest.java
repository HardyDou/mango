package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import io.mango.payment.core.service.IPaymentChannelAdapter;
import io.mango.payment.core.service.PaymentChannelAdapterRegistry;
import io.mango.payment.core.service.PaymentChannelSyncService;
import io.mango.payment.core.service.PaymentNumberService;
import io.mango.payment.core.service.PaymentOrderStateService;
import io.mango.payment.core.service.PaymentOrderStatusFlowService;
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

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PaymentCashierServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_cashier_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentCashierServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentCashierConfigMapper cashierConfigMapper;

    @Autowired
    private PaymentBusinessOrderMapper businessOrderMapper;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;

    @Autowired
    private PaymentCashierServiceImpl service;

    @Autowired
    private TestPaymentChannelAdapter mangoPayAdapter;

    @Autowired
    private TestOfflinePaymentChannelAdapter offlineAdapter;

    @Autowired
    private TestPaymentOrderStatusFlowService statusFlowService;

    @Autowired
    private TestPaymentChannelSyncService channelSyncService;

    @Autowired
    private TestPaymentNumberService numberService;

    @BeforeEach
    void setUp() {
        resetSchema();
        seedBaseData();
        mangoPayAdapter.clear();
        offlineAdapter.clear();
        statusFlowService.clear();
        channelSyncService.clear();
        numberService.nextValue = "PO2026060600000001";
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void detailSessionUsesRealCashierOrderRouteSqlAndMasksSubjectValues() {
        var result = service.detailSession(350001L, 360001L).getData();

        assertThat(cashierConfigMapper.selectByIdIgnoreTenant(350001L).getTenantId()).isEqualTo(1L);
        assertThat(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L).getBizOrderNo())
                .isEqualTo("BO202606060001");
        assertThat(contractCapabilityMapper.selectRoutedCashierCapability(
                1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", 9900L).getChannelCode())
                .isEqualTo("MANGO_PAY");
        assertThat(result.getSubject().getCreditCode()).isEqualTo("9131****001X");
        assertThat(result.getSubject().getBankAccountNo()).isEqualTo("6222****0001");
        assertThat(result.getMethods())
                .extracting("methodCode", "categoryCode", "channelCode")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("PERSONAL_WECHAT_QR", "WECHAT_PAY", "MANGO_PAY"));
    }

    @Test
    void payPersistsPaymentOrderUpdatesApplyResultAndAdvancesBusinessOrderThroughRealMappers() {
        PaymentCashierPayResultVO result = service.pay(payCommand("PERSONAL_WECHAT_QR")).getData();

        PaymentOrderEntity order = paymentOrderMapper.selectByPayOrderNo("PO2026060600000001");
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectById(360001L);
        assertThat(order.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(order.getCashierConfigId()).isEqualTo(350001L);
        assertThat(order.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(order.getContractId()).isEqualTo(331001L);
        assertThat(order.getContractCapabilityId()).isEqualTo(333001L);
        assertThat(order.getRouteRuleId()).isEqualTo(334001L);
        assertThat(order.getMethodId()).isEqualTo(340001L);
        assertThat(order.getStatus()).isEqualTo("PAYING");
        assertThat(order.getChannelTradeNo()).isEqualTo("TEST-PO2026060600000001");
        assertThat(paymentMaterialJson("PO2026060600000001")).contains("test-pay:PO2026060600000001");
        assertThat(businessOrder.getStatus()).isEqualTo("PAYING");
        assertThat(result.getStatus()).isEqualTo("PAYING");
        assertThat(result.getMaterial().getQrContent()).isEqualTo("test-pay:PO2026060600000001");
        assertThat(mangoPayAdapter.lastPaymentCommand.contractConfigValuesJson()).isEqualTo("{\"merchantNo\":\"MANGO_PAY_MERCHANT_001\"}");
        assertThat(statusFlowService.records).containsExactly(
                "PO2026060600000001:null->CREATED",
                "PO2026060600000001:CREATED->PAYING");
    }

    @Test
    void payOfflineTransferPassesContractAccountConfigToChannelAdapter() {
        switchCashierToOfflineOnly();
        numberService.nextValue = "PO2026060600000002";

        PaymentCashierPayResultVO result = service.pay(payCommand("CORPORATE_OFFLINE_ACCOUNT")).getData();

        PaymentOrderEntity order = paymentOrderMapper.selectByPayOrderNo("PO2026060600000002");
        assertThat(result.getChannelCode()).isEqualTo("OFFLINE_COLLECTION");
        assertThat(order.getChannelCode()).isEqualTo("OFFLINE_COLLECTION");
        assertThat(order.getContractId()).isEqualTo(331004L);
        assertThat(offlineAdapter.lastPaymentCommand.contractConfigValuesJson())
                .isEqualTo("{\"accountName\":\"芒果科技签约户\",\"accountNo\":\"enc:offline-account\",\"bankName\":\"签约开户行\"}");
        assertThat(offlineAdapter.lastPaymentCommand.subjectId()).isEqualTo(320001L);
        assertThat(offlineAdapter.lastPaymentCommand.subjectName()).isEqualTo("芒果科技有限公司");
        assertThat(offlineAdapter.createdOrder.getPayOrderNo()).isEqualTo("PO2026060600000002");
    }

    @Test
    void payReturnsExistingProcessingResultThroughRealPayResultSql() {
        insertProcessingPaymentOrder("PO2026060600000099");

        PaymentCashierPayResultVO result = service.pay(payCommand("PERSONAL_WECHAT_QR")).getData();

        assertThat(result.getPayOrderNo()).isEqualTo("PO2026060600000099");
        assertThat(result.getStatus()).isEqualTo("PAYING");
        assertThat(result.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(result.getMaterial().getQrContent()).isEqualTo("existing-qr");
        assertThat(mangoPayAdapter.lastPaymentCommand).isNull();
    }

    @Test
    void payRejectsUnavailableMethodBeforeCreatingPaymentOrder() {
        assertThatThrownBy(() -> service.pay(payCommand("CORPORATE_EBANK_REDIRECT")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前订单不可使用该支付方式");

        assertThat(paymentOrderMapper.selectByPayOrderNo("PO2026060600000001")).isNull();
    }

    private PaymentCashierPayCommand payCommand(String methodCode) {
        PaymentCashierPayCommand command = new PaymentCashierPayCommand();
        command.setCashierConfigId(350001L);
        command.setBusinessOrderId(360001L);
        command.setMethodCode(methodCode);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("""
                create alias if not exists FIND_IN_SET for
                "io.mango.payment.core.service.impl.PaymentCashierServiceImplIntegrationTest.findInSet"
                """);
        jdbcTemplate.execute("drop table if exists payment_transaction_flow");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_business_order");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule_item");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_capability");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_channel_capability");
        jdbcTemplate.execute("drop table if exists payment_channel");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        jdbcTemplate.execute("drop table if exists payment_application");
        createReferenceTables();
        createRouteTables();
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
                    credit_code varchar(128),
                    credit_code_hash varchar(128),
                    bank_account_no varchar(128),
                    bank_name varchar(128),
                    license_file_id bigint,
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
                    channel_id bigint,
                    account_nature varchar(64),
                    instrument_type varchar(64),
                    interaction_type varchar(64),
                    terminal_scope varchar(128),
                    payment_material_type varchar(64),
                    cashier_group_code varchar(64),
                    cashier_group_name varchar(64),
                    cashier_group_sort int,
                    icon_file_id bigint,
                    requires_bank_selection int,
                    requires_qr_refresh int,
                    description varchar(512),
                    sort int,
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

    private void createRouteTables() {
        jdbcTemplate.execute("""
                create table payment_channel (
                    id bigint primary key,
                    channel_code varchar(128),
                    channel_name varchar(128),
                    environment varchar(64),
                    channel_type varchar(64),
                    adapter_type varchar(64),
                    gateway_base_url varchar(512),
                    field_template_json varchar(2048),
                    capability_summary varchar(512),
                    bill_fetch_modes varchar(256),
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
                create table payment_channel_capability (
                    id bigint primary key,
                    channel_id bigint,
                    method_code varchar(128),
                    terminal_type varchar(64),
                    environment varchar(64),
                    supports_refund int,
                    supports_query int,
                    supports_close int,
                    supports_bill int,
                    supports_reconcile int,
                    min_amount bigint,
                    max_amount bigint,
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
                    environment varchar(64),
                    merchant_no varchar(128),
                    app_id varchar(128),
                    config_values_json varchar(1024),
                    enabled_method_codes varchar(512),
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
                create table payment_channel_contract_capability (
                    id bigint primary key,
                    contract_id bigint,
                    channel_capability_id bigint,
                    method_code varchar(128),
                    terminal_type varchar(64),
                    fee_rate decimal(18,6),
                    min_amount bigint,
                    max_amount bigint,
                    priority int,
                    certificate_expire_time timestamp,
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
                create table payment_method_route_rule (
                    id bigint primary key,
                    rule_code varchar(128),
                    rule_name varchar(128),
                    app_id bigint,
                    subject_id bigint,
                    method_code varchar(128),
                    terminal_type varchar(64),
                    environment varchar(64),
                    route_mode varchar(64),
                    fallback_enabled int,
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
                create table payment_method_route_rule_item (
                    id bigint primary key,
                    rule_id bigint,
                    contract_capability_id bigint,
                    priority int,
                    weight int,
                    min_amount bigint,
                    max_amount bigint,
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
                    id bigint primary key,
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
                create table payment_transaction_flow (
                    id bigint primary key,
                    flow_no varchar(128),
                    payment_order_id bigint,
                    tenant_id bigint,
                    created_at timestamp default current_timestamp
                )
                """);
    }

    private void seedBaseData() {
        jdbcTemplate.update("""
                insert into payment_application (id, app_id, app_name, status, tenant_id, del_flag)
                values (310001, 'app_order_center', '订单中心', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_enterprise_subject
                    (id, subject_name, credit_code, bank_account_no, bank_name, status, tenant_id, del_flag)
                values (320001, '芒果科技有限公司', 'enc:credit-ciphertext', 'enc:account-ciphertext', '招商银行', 1, 1, 0)
                """);
        jdbcTemplate.update("""
                insert into payment_cashier_config (
                    id, cashier_name, application_id, default_cashier, enterprise_subject_ids, method_codes,
                    default_method_code, method_display_order, result_return_url, display_config, status, tenant_id, del_flag
                ) values (
                    350001, '订单中心 Web 收银台', 310001, 1, '320001',
                    'PERSONAL_WECHAT_QR', 'PERSONAL_WECHAT_QR', 'PERSONAL_WECHAT_QR',
                    'https://order.example.com/result', '{"subtitle":"测试收银台"}', 1, 1, 0
                )
                """);
        insertMethod(340001L, "PERSONAL_WECHAT_QR", "微信扫码", "PERSONAL", "WECHAT", "QR_CODE", "QR", "WECHAT_PAY", "微信支付", 10);
        insertMethod(340004L, "CORPORATE_OFFLINE_ACCOUNT", "线下转账", "CORPORATE", "OFFLINE_TRANSFER", "ACCOUNT_TRANSFER", "TRANSFER_ACCOUNT", "OFFLINE_TRANSFER", "线下转账", 40);
        insertChannelAndContract(330001L, "MANGO_PAY", "芒果支付", 331001L, "{\"merchantNo\":\"MANGO_PAY_MERCHANT_001\"}");
        insertChannelAndContract(330004L, "OFFLINE_COLLECTION", "线下收款", 331004L,
                "{\"accountName\":\"芒果科技签约户\",\"accountNo\":\"enc:offline-account\",\"bankName\":\"签约开户行\"}");
        insertCapabilityRoute(332001L, 330001L, 333001L, 331001L, "PERSONAL_WECHAT_QR", 334001L, 335001L);
        insertCapabilityRoute(332004L, 330004L, 333004L, 331004L, "CORPORATE_OFFLINE_ACCOUNT", 334004L, 335004L);
        jdbcTemplate.update("""
                insert into payment_business_order (
                    id, biz_order_no, app_code, title, subject_id, amount, paid_amount, refunded_amount,
                    currency, status, expire_time, return_url, tenant_id, del_flag
                ) values (
                    360001, 'BO202606060001', 'app_order_center', '测试订单', 320001, 9900, 0, 0,
                    'CNY', 'TO_PAY', ?, 'https://order.example.com/return', 1, 0
                )
                """, LocalDateTime.now().plusMinutes(30));
    }

    private void insertMethod(
            Long id,
            String methodCode,
            String methodName,
            String accountNature,
            String instrumentType,
            String interactionType,
            String paymentMaterialType,
            String cashierGroupCode,
            String cashierGroupName,
            Integer cashierGroupSort) {
        jdbcTemplate.update("""
                insert into payment_method (
                    id, method_code, method_name, account_nature, instrument_type, interaction_type,
                    terminal_scope, payment_material_type, cashier_group_code, cashier_group_name,
                    cashier_group_sort, status, tenant_id, del_flag
                ) values (?, ?, ?, ?, ?, ?, 'WEB,H5', ?, ?, ?, ?, 1, 1, 0)
                """, id, methodCode, methodName, accountNature, instrumentType, interactionType,
                paymentMaterialType, cashierGroupCode, cashierGroupName, cashierGroupSort);
    }

    private void insertChannelAndContract(Long channelId, String channelCode, String channelName, Long contractId, String configValuesJson) {
        jdbcTemplate.update("""
                insert into payment_channel (id, channel_code, channel_name, environment, channel_type, adapter_type, status, tenant_id, del_flag)
                values (?, ?, ?, 'PROD', 'AGGREGATOR', ?, 1, 1, 0)
                """, channelId, channelCode, channelName, channelCode);
        jdbcTemplate.update("""
                insert into payment_channel_contract (
                    id, contract_code, contract_name, subject_id, channel_id, environment, merchant_no,
                    config_values_json, enabled_method_codes, status, tenant_id, del_flag
                ) values (?, ?, ?, 320001, ?, 'PROD', ?, ?, null, 1, 1, 0)
                """, contractId, channelCode + "_CONTRACT", channelName + "签约", channelId, channelCode + "_MERCHANT_001", configValuesJson);
    }

    private void insertCapabilityRoute(
            Long channelCapabilityId,
            Long channelId,
            Long contractCapabilityId,
            Long contractId,
            String methodCode,
            Long routeRuleId,
            Long routeItemId) {
        jdbcTemplate.update("""
                insert into payment_channel_capability (
                    id, channel_id, method_code, terminal_type, environment, supports_refund, supports_query,
                    supports_close, supports_bill, supports_reconcile, min_amount, max_amount, status, tenant_id, del_flag
                ) values (?, ?, ?, 'WEB', 'PROD', 1, 1, 1, 1, 1, 1, 500000, 1, 1, 0)
                """, channelCapabilityId, channelId, methodCode);
        jdbcTemplate.update("""
                insert into payment_channel_contract_capability (
                    id, contract_id, channel_capability_id, method_code, terminal_type, fee_rate,
                    min_amount, max_amount, priority, status, tenant_id, del_flag
                ) values (?, ?, ?, ?, 'WEB', 0.006, 1, 500000, 10, 1, 1, 0)
                """, contractCapabilityId, contractId, channelCapabilityId, methodCode);
        jdbcTemplate.update("""
                insert into payment_method_route_rule (
                    id, rule_code, rule_name, app_id, subject_id, method_code, terminal_type, environment,
                    route_mode, fallback_enabled, status, tenant_id, del_flag
                ) values (?, ?, ?, 310001, 320001, ?, 'WEB', 'PROD', 'FIXED', 1, 1, 1, 0)
                """, routeRuleId, methodCode + "_RULE", methodCode + "路由", methodCode);
        jdbcTemplate.update("""
                insert into payment_method_route_rule_item (
                    id, rule_id, contract_capability_id, priority, weight, min_amount, max_amount, status, tenant_id, del_flag
                ) values (?, ?, ?, 10, 100, 1, 500000, 1, 1, 0)
                """, routeItemId, routeRuleId, contractCapabilityId);
    }

    private void switchCashierToOfflineOnly() {
        jdbcTemplate.update("""
                update payment_cashier_config
                set method_codes = 'CORPORATE_OFFLINE_ACCOUNT',
                    default_method_code = 'CORPORATE_OFFLINE_ACCOUNT',
                    method_display_order = 'CORPORATE_OFFLINE_ACCOUNT'
                where id = 350001
                """);
    }

    private void insertProcessingPaymentOrder(String payOrderNo) {
        jdbcTemplate.update("""
                insert into payment_order (
                    id, pay_order_no, business_order_id, cashier_config_id, channel_id, channel_code,
                    channel_merchant_no, contract_id, contract_capability_id, route_rule_id, method_id,
                    amount, status, channel_trade_no, payment_material_json, success_flag, expire_time, tenant_id
                ) values (
                    370099, ?, 360001, 350001, 330001, 'MANGO_PAY',
                    'MANGO_PAY_MERCHANT_001', 331001, 333001, 334001, 340001,
                    9900, 'PAYING', 'TEST-EXISTING', '{"materialType":"QR","qrContent":"existing-qr"}', 0, ?, 1
                )
                """, payOrderNo, LocalDateTime.now().plusMinutes(30));
    }

    private String paymentMaterialJson(String payOrderNo) {
        return jdbcTemplate.queryForObject(
                "select payment_material_json from payment_order where pay_order_no = ?",
                String.class,
                payOrderNo);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentCashierConfigMapper.class)
    @Import(PaymentCashierServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentChannelAdapter mangoPayAdapter() {
            return new TestPaymentChannelAdapter();
        }

        @Bean
        TestOfflinePaymentChannelAdapter offlineAdapter() {
            return new TestOfflinePaymentChannelAdapter();
        }

        @Bean
        PaymentChannelAdapterRegistry channelAdapterRegistry(
                TestPaymentChannelAdapter mangoPayAdapter,
                TestOfflinePaymentChannelAdapter offlineAdapter) {
            return new PaymentChannelAdapterRegistry(List.of(mangoPayAdapter, offlineAdapter));
        }

        @Bean
        PaymentOrderStateService paymentOrderStateService() {
            return new PaymentOrderStateService();
        }

        @Bean
        TestPaymentOrderStatusFlowService paymentOrderStatusFlowService() {
            return new TestPaymentOrderStatusFlowService();
        }

        @Bean
        TestPaymentChannelSyncService paymentChannelSyncService() {
            return new TestPaymentChannelSyncService();
        }

        @Bean
        PaymentSensitiveValueService paymentSensitiveValueService() {
            return new PaymentSensitiveValueService(null) {
                @Override
                public String mask(String value, int left, int right) {
                    if ("enc:credit-ciphertext".equals(value)) {
                        return "9131****001X";
                    }
                    if ("enc:account-ciphertext".equals(value)) {
                        return "6222****0001";
                    }
                    return value;
                }
            };
        }

        @Bean
        TestPaymentNumberService paymentNumberService() {
            return new TestPaymentNumberService();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    static class TestPaymentNumberService extends PaymentNumberService {

        private String nextValue;

        TestPaymentNumberService() {
            super(null);
        }

        @Override
        public String next(String type) {
            return nextValue;
        }
    }

    static class TestPaymentOrderStatusFlowService extends PaymentOrderStatusFlowService {

        private final List<String> records = new ArrayList<>();

        TestPaymentOrderStatusFlowService() {
            super(null);
        }

        @Override
        public void record(
                Long tenantId,
                String orderType,
                Long orderId,
                String orderNo,
                String fromStatus,
                String toStatus,
                String sourceType,
                String sourceNo,
                LocalDateTime occurredAt,
                String remark) {
            records.add(orderNo + ":" + fromStatus + "->" + toStatus);
        }

        void clear() {
            records.clear();
        }
    }

    static class TestPaymentChannelSyncService extends PaymentChannelSyncService {

        private final List<String> synced = new ArrayList<>();

        TestPaymentChannelSyncService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public PaymentSyncResult syncPaymentStatus(String payOrderNo) {
            synced.add(payOrderNo);
            return new PaymentSyncResult(payOrderNo, "PAYING", null, false, 0, "TEST");
        }

        void clear() {
            synced.clear();
        }
    }

    static class TestPaymentChannelAdapter implements IPaymentChannelAdapter {

        protected IPaymentChannelAdapter.PaymentApplyCommand lastPaymentCommand;
        protected PaymentOrderEntity createdOrder;

        @Override
        public String channelCode() {
            return "MANGO_PAY";
        }

        @Override
        public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
            this.lastPaymentCommand = command;
            PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
            material.setMaterialType("QR");
            material.setQrContent("test-pay:" + command.payOrderNo());
            return new PaymentApplyResult(
                    "SUCCESS",
                    "MANGO_PAY_SUCCESS",
                    "SYNC_SUCCESS",
                    "SUCCESS",
                    "TEST-" + command.payOrderNo(),
                    material);
        }

        @Override
        public void afterPaymentOrderCreated(PaymentApplyCommand command, PaymentApplyResult result, PaymentOrderEntity order) {
            this.createdOrder = order;
        }

        @Override
        public RefundApplyResult applyRefund(RefundApplyCommand command) {
            throw new AssertionError("Refund is outside this cashier test.");
        }

        @Override
        public ChannelBillResult generateBill(ChannelBillCommand command) {
            return new ChannelBillResult(List.<PaymentChannelBillItemRow>of());
        }

        @Override
        public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
            throw new AssertionError("Query is outside this cashier test.");
        }

        @Override
        public RefundQueryResult queryRefund(RefundQueryCommand command) {
            throw new AssertionError("Refund query is outside this cashier test.");
        }

        void clear() {
            lastPaymentCommand = null;
            createdOrder = null;
        }
    }

    static class TestOfflinePaymentChannelAdapter extends TestPaymentChannelAdapter {

        @Override
        public String channelCode() {
            return "OFFLINE_COLLECTION";
        }

        @Override
        public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
            this.lastPaymentCommand = command;
            PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
            material.setMaterialType("TRANSFER_ACCOUNT");
            material.setAccountName("签约收款户");
            material.setAccountNo("6222000000000001");
            material.setBankName("签约开户行");
            material.setTransferRemark("A1b2C3");
            return new PaymentApplyResult(
                    "SUCCESS",
                    "OFFLINE_COLLECTION_CREATED",
                    "SYNC_SUCCESS",
                    "SUCCESS",
                    "TEST-" + command.payOrderNo(),
                    material);
        }
    }
}
