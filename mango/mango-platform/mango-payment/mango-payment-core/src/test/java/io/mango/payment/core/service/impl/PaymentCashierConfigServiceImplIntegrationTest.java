package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
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
        PaymentCashierConfigServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_cashier_config_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentCashierConfigServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentCashierConfigMapper cashierConfigMapper;

    @Autowired
    private PaymentCashierConfigServiceImpl service;

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
    void createCashierConfigValidatesRelationsAndPersistsNormalizedConfigThroughRealMappers() {
        insertValidRelations();

        Long id = service.createCashierConfig(command()).getData();

        PaymentCashierConfig entity = cashierConfigMapper.selectById(id);
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getCashierName()).isEqualTo("订单中心 Web 收银台");
        assertThat(entity.getApplicationId()).isEqualTo(310001L);
        assertThat(entity.getDefaultCashier()).isEqualTo(1);
        assertThat(entity.getEnterpriseSubjectIds()).isEqualTo("320001");
        assertThat(entity.getMethodCodes()).isEqualTo("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        assertThat(entity.getDefaultMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(entity.getMethodDisplayOrder()).isEqualTo("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        assertThat(entity.getStatus()).isEqualTo(1);
        assertThat(auditService.records).containsExactly(
                "CREATE_CASHIER_CONFIG|PAYMENT_CASHIER_CONFIG|" + id + "|SUCCESS");
    }

    @Test
    void createCashierConfigRejectsDuplicateEnabledDefaultCashierThroughRealSelectCount() {
        insertValidRelations();
        insertCashierConfig(350001L, 310001L, 1, 1, 0);

        assertThatThrownBy(() -> service.createCashierConfig(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("同一应用只能启用一个默认收银台");

        assertThat(activeCashierCount()).isEqualTo(1L);
    }

    @Test
    void createCashierConfigRejectsDisplayOrderOutsideVisibleMethodCodesBeforeInsert() {
        insertValidRelations();
        SavePaymentCashierConfigCommand command = command();
        command.setMethodDisplayOrder("PERSONAL_ALIPAY_H5");

        assertThatThrownBy(() -> service.createCashierConfig(command))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式展示顺序必须包含在可见支付方式中");

        assertThat(activeCashierCount()).isZero();
    }

    @Test
    void deleteCashierConfigRejectsWhenPaymentOrderReferencesConfigThroughRealMapperSql() {
        insertValidRelations();
        insertCashierConfig(350001L, 310001L, 1, 1, 0);
        insertPaymentOrder(360001L, 350001L);

        assertThatThrownBy(() -> service.deleteCashierConfig(350001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CASHIER_CONFIG_DELETE_HAS_RELATIONS.getMessage());

        assertThat(cashierConfigMapper.selectById(350001L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_CASHIER_CONFIG|PAYMENT_CASHIER_CONFIG|350001|REJECTED");
    }

    @Test
    void deleteCashierConfigLogicalDeletesWhenNoRelationsThroughRealMapperSql() {
        insertValidRelations();
        insertCashierConfig(350001L, 310001L, 1, 1, 0);

        service.deleteCashierConfig(350001L);

        assertThat(cashierConfigMapper.selectById(350001L)).isNull();
        assertThat(countDeletedCashierConfigs()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "DELETE_CASHIER_CONFIG|PAYMENT_CASHIER_CONFIG|350001|SUCCESS");
    }

    private SavePaymentCashierConfigCommand command() {
        SavePaymentCashierConfigCommand command = new SavePaymentCashierConfigCommand();
        command.setCashierName("  订单中心 Web 收银台  ");
        command.setApplicationId(310001L);
        command.setDefaultCashier(1);
        command.setEnterpriseSubjectIds(" 320001 ");
        command.setMethodCodes("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        command.setDefaultMethodCode("PERSONAL_WECHAT_QR");
        command.setMethodDisplayOrder("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        command.setDisplayConfig("{\"subtitle\":\"请确认订单金额后选择支付方式\"}");
        command.setStatus(1);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists payment_virtual_channel_payment");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        jdbcTemplate.execute("drop table if exists payment_application");
        createApplicationTable();
        createEnterpriseSubjectTable();
        createMethodTable();
        createCashierConfigTable();
        createRelationTables();
    }

    private void createApplicationTable() {
        jdbcTemplate.execute("""
                create table payment_application (
                    id bigint primary key,
                    app_id varchar(128),
                    app_name varchar(128),
                    app_secret varchar(512),
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
    }

    private void createEnterpriseSubjectTable() {
        jdbcTemplate.execute("""
                create table payment_enterprise_subject (
                    id bigint primary key,
                    subject_name varchar(128),
                    credit_code varchar(512),
                    credit_code_hash varchar(128),
                    bank_account_no varchar(512),
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
    }

    private void createMethodTable() {
        jdbcTemplate.execute("""
                create table payment_method (
                    id bigint primary key,
                    method_code varchar(128),
                    method_name varchar(128),
                    channel_id bigint,
                    account_nature varchar(64),
                    instrument_type varchar(64),
                    interaction_type varchar(64),
                    terminal_scope varchar(64),
                    payment_material_type varchar(64),
                    cashier_group_code varchar(64),
                    cashier_group_name varchar(128),
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

    private void createCashierConfigTable() {
        jdbcTemplate.execute("""
                create table payment_cashier_config (
                    id bigint primary key,
                    cashier_name varchar(128),
                    application_id bigint,
                    default_cashier int,
                    enterprise_subject_ids varchar(512),
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
    }

    private void createRelationTables() {
        jdbcTemplate.execute("""
                create table payment_order (
                    id bigint primary key,
                    cashier_config_id bigint,
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_virtual_channel_payment (
                    id bigint primary key,
                    cashier_config_id bigint,
                    tenant_id bigint
                )
                """);
    }

    private void insertValidRelations() {
        jdbcTemplate.update("""
                        insert into payment_application (
                            id, app_id, app_name, secret_configured, secret_version, sign_algorithm,
                            ip_whitelist_enabled, payload_encrypt_enabled, demo_app, status, tenant_id, del_flag,
                            created_at, updated_at
                        ) values (310001, 'app_order_center', '订单中心', 0, 0, 'HMAC_SHA256',
                            0, 0, 0, 1, 1, 0, current_timestamp, current_timestamp)
                        """);
        jdbcTemplate.update("""
                        insert into payment_enterprise_subject (
                            id, subject_name, credit_code, credit_code_hash, bank_account_no, bank_name,
                            status, tenant_id, del_flag, created_at, updated_at
                        ) values (320001, '芒果科技有限公司', 'enc:credit', 'hash:credit', 'enc:account',
                            '招商银行', 1, 1, 0, current_timestamp, current_timestamp)
                        """);
        insertMethod(340001L, "PERSONAL_WECHAT_QR", "微信扫码");
        insertMethod(340002L, "CORPORATE_OFFLINE_ACCOUNT", "线下转账");
    }

    private void insertMethod(Long id, String methodCode, String methodName) {
        jdbcTemplate.update("""
                        insert into payment_method (
                            id, method_code, method_name, channel_id, status, tenant_id, del_flag,
                            created_at, updated_at
                        ) values (?, ?, ?, 330001, 1, 1, 0, current_timestamp, current_timestamp)
                        """,
                id, methodCode, methodName);
    }

    private void insertCashierConfig(Long id, Long applicationId, Integer defaultCashier, Integer status, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_cashier_config (
                            id, cashier_name, application_id, default_cashier, enterprise_subject_ids,
                            method_codes, default_method_code, method_display_order, status, tenant_id,
                            del_flag, created_at, updated_at
                        ) values (?, '订单中心 Web 收银台', ?, ?, '320001',
                            'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT', 'PERSONAL_WECHAT_QR',
                            'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT', ?, 1, ?,
                            current_timestamp, current_timestamp)
                        """,
                id, applicationId, defaultCashier, status, delFlag);
    }

    private void insertPaymentOrder(Long id, Long cashierConfigId) {
        jdbcTemplate.update("""
                        insert into payment_order (id, cashier_config_id, tenant_id)
                        values (?, ?, 1)
                        """,
                id, cashierConfigId);
    }

    private Long activeCashierCount() {
        return cashierConfigMapper.selectCount(new LambdaQueryWrapper<PaymentCashierConfig>()
                .eq(PaymentCashierConfig::getDelFlag, 0));
    }

    private Long countDeletedCashierConfigs() {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_cashier_config where id = 350001 and del_flag = 1",
                Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentCashierConfigMapper.class)
    @Import(PaymentCashierConfigServiceImpl.class)
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
