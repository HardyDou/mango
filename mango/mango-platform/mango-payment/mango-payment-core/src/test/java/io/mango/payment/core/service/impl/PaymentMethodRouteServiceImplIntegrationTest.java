package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleItemCommand;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;
import io.mango.payment.core.entity.PaymentMethodRouteRule;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItem;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleItemMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
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
import org.springframework.test.context.TestPropertySource;

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
        PaymentMethodRouteServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_method_route_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentMethodRouteServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentMethodRouteRuleMapper routeRuleMapper;

    @Autowired
    private PaymentMethodRouteRuleItemMapper routeRuleItemMapper;

    @Autowired
    private PaymentMethodRouteServiceImpl service;

    @Autowired
    private TestPaymentOperationAuditService auditService;

    @BeforeEach
    void setUp() {
        resetSchema();
        auditService.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createRouteRuleSavesRuleItemsAndAuditsThroughRealMappers() {
        insertReferenceData();

        Long id = service.createRouteRule(command()).getData();

        PaymentMethodRouteRule rule = routeRuleMapper.selectById(id);
        assertThat(rule.getTenantId()).isEqualTo(1L);
        assertThat(rule.getRuleCode()).isEqualTo("ORDER_CENTER_WECHAT_MANGO_PAY_TEST");
        assertThat(rule.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(rule.getEnvironment()).isEqualTo("MANGO_PAY");
        PaymentMethodRouteRuleItem item = singleRouteItem(id);
        assertThat(item.getContractCapabilityId()).isEqualTo(333001L);
        assertThat(item.getPriority()).isEqualTo(10);
        assertThat(item.getWeight()).isEqualTo(100);
        assertThat(auditService.records).containsExactly(
                "CREATE_METHOD_ROUTE|PAYMENT_METHOD_ROUTE|ORDER_CENTER_WECHAT_MANGO_PAY_TEST|SUCCESS");
    }

    @Test
    void deleteRouteRuleRejectsWhenPaymentOrderReferencesRouteThroughRealMapperSql() {
        insertReferenceData();
        insertRouteRule(334001L, 1);
        insertRouteItem(335001L, 334001L, 333001L, 1L, 500000L, 1);
        jdbcTemplate.update("insert into payment_order (id, route_rule_id, tenant_id) values (360001, 334001, 1)");

        assertThatThrownBy(() -> service.deleteRouteRule(334001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_METHOD_ROUTE_DELETE_HAS_RELATIONS.getMessage());

        assertThat(routeRuleMapper.selectById(334001L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_METHOD_ROUTE|PAYMENT_METHOD_ROUTE|ORDER_CENTER_WECHAT_MANGO_PAY|REJECTED");
    }

    @Test
    void trialRouteReturnsMatchedRuleAndItemFromRealJoinSql() {
        insertReferenceData();
        insertRouteRule(334001L, 1);
        insertRouteItem(335001L, 334001L, 333001L, 1L, 500000L, 1);

        PaymentMethodRouteTrialVO result = service.trialRoute(trialCommand(9900L)).getData();

        assertThat(result.getMatched()).isTrue();
        assertThat(result.getMatchedRule().getRuleCode()).isEqualTo("ORDER_CENTER_WECHAT_MANGO_PAY");
        assertThat(result.getMatchedItem().getContractCapabilityId()).isEqualTo(333001L);
        assertThat(result.getFilterReasons()).isEmpty();
    }

    @Test
    void trialRouteReturnsFilterReasonWhenCapabilityAmountExceededFromRealJoinSql() {
        insertReferenceData();
        jdbcTemplate.update("update payment_channel_contract_capability set max_amount = 100 where id = 333001");
        insertRouteRule(334001L, 1);
        insertRouteItem(335001L, 334001L, 333001L, 1L, 500000L, 1);

        PaymentMethodRouteTrialVO result = service.trialRoute(trialCommand(9900L)).getData();

        assertThat(result.getMatched()).isFalse();
        assertThat(result.getFilterReasons()).anyMatch(reason -> reason.contains("超过签约能力最大金额"));
        assertThat(auditService.records).containsExactly(
                "TRIAL_METHOD_ROUTE|PAYMENT_METHOD_ROUTE|PERSONAL_WECHAT_QR|REJECTED");
    }

    @Test
    void createRouteRuleRejectsWhenContractCapabilityCannotResolveRouteEnvironmentThroughRealSql() {
        insertReferenceData();
        jdbcTemplate.update("update payment_channel_capability set status = 0 where id = 332001");
        SavePaymentMethodRouteRuleCommand command = command();
        command.setEnvironment(null);

        assertThatThrownBy(() -> service.createRouteRule(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getMessage());

        assertThat(routeRuleMapper.selectCount(null)).isZero();
        assertThat(routeRuleItemMapper.selectCount(null)).isZero();
    }

    private SavePaymentMethodRouteRuleCommand command() {
        SavePaymentMethodRouteRuleCommand command = new SavePaymentMethodRouteRuleCommand();
        command.setRuleCode("ORDER_CENTER_WECHAT_MANGO_PAY_TEST");
        command.setRuleName("订单中心微信芒果支付路由测试");
        command.setAppId(310001L);
        command.setSubjectId(320001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setRouteMode("PRIORITY");
        command.setFallbackEnabled(1);
        command.setStatus(1);
        command.setItems(List.of(itemCommand()));
        return command;
    }

    private SavePaymentMethodRouteRuleItemCommand itemCommand() {
        SavePaymentMethodRouteRuleItemCommand item = new SavePaymentMethodRouteRuleItemCommand();
        item.setContractCapabilityId(333001L);
        item.setPriority(10);
        item.setWeight(100);
        item.setMinAmount(1L);
        item.setMaxAmount(500000L);
        item.setStatus(1);
        return item;
    }

    private PaymentMethodRouteTrialCommand trialCommand(Long amount) {
        PaymentMethodRouteTrialCommand command = new PaymentMethodRouteTrialCommand();
        command.setApplicationId(310001L);
        command.setSubjectId(320001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setAmount(amount);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule_item");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_capability");
        jdbcTemplate.execute("drop table if exists payment_channel_capability");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_channel");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        jdbcTemplate.execute("drop table if exists payment_application");
        createReferenceTables();
        createRouteTables();
    }

    private void createReferenceTables() {
        jdbcTemplate.execute("""
                create table payment_application (
                    id bigint primary key, app_id varchar(128), app_name varchar(128), app_secret varchar(512),
                    secret_configured int, secret_version int, secret_last_reset_time timestamp,
                    sign_algorithm varchar(64), ip_whitelist_enabled int, ip_whitelist varchar(512),
                    payload_encrypt_enabled int, notify_retry_policy varchar(512), demo_app int, status int,
                    tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_enterprise_subject (
                    id bigint primary key, subject_name varchar(128), credit_code varchar(512), credit_code_hash varchar(128),
                    bank_account_no varchar(512), bank_name varchar(128), license_file_id bigint, status int,
                    tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_method (
                    id bigint primary key, method_code varchar(128), method_name varchar(128), channel_id bigint,
                    account_nature varchar(64), instrument_type varchar(64), interaction_type varchar(64),
                    terminal_scope varchar(64), payment_material_type varchar(64), cashier_group_code varchar(64),
                    cashier_group_name varchar(128), cashier_group_sort int, icon_file_id bigint,
                    requires_bank_selection int, requires_qr_refresh int, description varchar(512), sort int, status int,
                    tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel (
                    id bigint primary key, channel_code varchar(128), channel_name varchar(128), environment varchar(64), status int,
                    tenant_id bigint, del_flag int default 0, created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_contract (
                    id bigint primary key, contract_name varchar(128), channel_id bigint, environment varchar(64), status int,
                    tenant_id bigint, del_flag int default 0, created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_capability (
                    id bigint primary key, channel_id bigint, method_code varchar(128), terminal_type varchar(64), environment varchar(64),
                    min_amount bigint, max_amount bigint, status int, tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_contract_capability (
                    id bigint primary key, contract_id bigint, channel_capability_id bigint, method_code varchar(128), terminal_type varchar(64),
                    fee_rate decimal(18,6), min_amount bigint, max_amount bigint, priority int, certificate_expire_time timestamp,
                    status int, tenant_id bigint,
                    del_flag int default 0, created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createRouteTables() {
        jdbcTemplate.execute("""
                create table payment_method_route_rule (
                    id bigint primary key, rule_code varchar(128), rule_name varchar(128), app_id bigint, subject_id bigint,
                    method_code varchar(128), terminal_type varchar(64), environment varchar(64), route_mode varchar(64),
                    fallback_enabled int, status int, tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_method_route_rule_item (
                    id bigint primary key, rule_id bigint, contract_capability_id bigint, priority int, weight int,
                    min_amount bigint, max_amount bigint, status int, tenant_id bigint, del_flag int default 0,
                    created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("create table payment_order (id bigint primary key, route_rule_id bigint, tenant_id bigint)");
    }

    private void insertReferenceData() {
        jdbcTemplate.update("insert into payment_application (id, app_name, tenant_id, del_flag) values (310001, '订单中心', 1, 0)");
        jdbcTemplate.update("insert into payment_enterprise_subject (id, subject_name, tenant_id, del_flag) values (320001, '芒果科技有限公司', 1, 0)");
        jdbcTemplate.update("insert into payment_method (id, method_code, method_name, tenant_id, del_flag) values (340001, 'PERSONAL_WECHAT_QR', '微信扫码', 1, 0)");
        jdbcTemplate.update("insert into payment_channel (id, channel_code, channel_name, environment, status, tenant_id, del_flag) values (330001, 'MANGO_PAY', '芒果支付', 'MANGO_PAY', 1, 1, 0)");
        jdbcTemplate.update("insert into payment_channel_contract (id, contract_name, channel_id, environment, status, tenant_id, del_flag) values (331001, '芒果科技芒果支付签约', 330001, 'MANGO_PAY', 1, 1, 0)");
        jdbcTemplate.update("insert into payment_channel_capability (id, channel_id, method_code, terminal_type, environment, min_amount, max_amount, status, tenant_id, del_flag) values (332001, 330001, 'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 1, 500000, 1, 1, 0)");
        jdbcTemplate.update("insert into payment_channel_contract_capability (id, contract_id, channel_capability_id, method_code, terminal_type, fee_rate, min_amount, max_amount, priority, status, tenant_id, del_flag) values (333001, 331001, 332001, 'PERSONAL_WECHAT_QR', 'WEB', 0.006, 1, 500000, 10, 1, 1, 0)");
    }

    private void insertRouteRule(Long id, Integer status) {
        jdbcTemplate.update("""
                        insert into payment_method_route_rule (
                            id, rule_code, rule_name, app_id, subject_id, method_code, terminal_type, environment,
                            route_mode, fallback_enabled, status, tenant_id, del_flag
                        ) values (?, 'ORDER_CENTER_WECHAT_MANGO_PAY', '订单中心微信芒果支付路由', 310001, 320001,
                            'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, ?, 1, 0)
                        """, id, status);
    }

    private void insertRouteItem(Long id, Long ruleId, Long capabilityId, Long minAmount, Long maxAmount, Integer status) {
        jdbcTemplate.update("""
                        insert into payment_method_route_rule_item (
                            id, rule_id, contract_capability_id, priority, weight, min_amount, max_amount, status, tenant_id, del_flag
                        ) values (?, ?, ?, 10, 100, ?, ?, ?, 1, 0)
                        """, id, ruleId, capabilityId, minAmount, maxAmount, status);
    }

    private PaymentMethodRouteRuleItem singleRouteItem(Long ruleId) {
        return routeRuleItemMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentMethodRouteRuleItem>()
                .eq(PaymentMethodRouteRuleItem::getRuleId, ruleId)).get(0);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentMethodRouteRuleMapper.class)
    @Import(PaymentMethodRouteServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentOperationAuditService paymentOperationAuditService() {
            return new TestPaymentOperationAuditService();
        }
    }

    static class TestPaymentOperationAuditService extends PaymentOperationAuditService {

        private final List<String> records = new ArrayList<>();

        TestPaymentOperationAuditService() {
            super(null);
        }

        @Override
        public void record(String operationAction, String resourceType, String resourceId, String operationResult) {
            records.add(operationAction + "|" + resourceType + "|" + resourceId + "|" + operationResult);
        }

        void clear() {
            records.clear();
        }
    }
}
