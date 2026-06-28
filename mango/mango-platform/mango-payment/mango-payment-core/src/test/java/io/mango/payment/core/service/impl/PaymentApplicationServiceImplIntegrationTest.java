package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
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
        PaymentApplicationServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_application_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentApplicationServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentApplicationMapper applicationMapper;

    @Autowired
    private PaymentApplicationServiceImpl service;

    @Autowired
    private TestPaymentOperationAuditService auditService;

    @Autowired
    private TestPaymentSensitiveValueService sensitiveValueService;

    @BeforeEach
    void setUp() {
        resetSchema();
        auditService.clear();
        sensitiveValueService.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createApplicationEncryptsStoredSecretAndReturnsPlaintextOnlyOnceThroughRealMapper() {
        CreatePaymentApplicationCommand command = createCommand();

        String plaintextSecret = service.createApplication(command).getData().getAppSecret();

        assertThat(plaintextSecret).isNotBlank();
        PaymentApplication persisted = singleApplication();
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getTenantId()).isEqualTo(1L);
        assertThat(persisted.getAppId()).startsWith("app_");
        assertThat(persisted.getAppName()).isEqualTo("开放接口应用");
        assertThat(persisted.getPayloadEncryptEnabled()).isEqualTo(1);
        assertThat(persisted.getSecretConfigured()).isEqualTo(1);
        assertThat(persisted.getSecretVersion()).isEqualTo(1);
        assertThat(persisted.getSecretLastResetTime()).isNotNull();
        assertThat(persisted.getAppSecret()).isEqualTo("enc:" + plaintextSecret);
        assertThat(auditService.records).containsExactly(
                "CREATE_APPLICATION|PAYMENT_APPLICATION|" + persisted.getAppId() + "|SUCCESS");
        assertThat(sensitiveValueService.encryptedPlaintexts).containsExactly(plaintextSecret);
    }

    @Test
    void deleteApplicationRejectsWhenRelatedCashierConfigExistsThroughRealMapperSql() {
        insertApplication(310001L, "app_order_center");
        insertCashierConfig(350001L, 310001L, 1L, 0);

        assertThatThrownBy(() -> service.deleteApplication(310001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_APPLICATION_DELETE_HAS_RELATIONS.getMessage());

        assertThat(applicationMapper.selectById(310001L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_APPLICATION|PAYMENT_APPLICATION|app_order_center|REJECTED");
    }

    @Test
    void deleteApplicationLogicalDeletesWhenNoRelationsThroughRealMapperSql() {
        insertApplication(310001L, "app_order_center");
        insertCashierConfig(350001L, 310001L, 1L, 1);

        service.deleteApplication(310001L);

        assertThat(applicationMapper.selectById(310001L)).isNull();
        assertThat(countDeletedApplications()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "DELETE_APPLICATION|PAYMENT_APPLICATION|app_order_center|SUCCESS");
    }

    private CreatePaymentApplicationCommand createCommand() {
        CreatePaymentApplicationCommand command = new CreatePaymentApplicationCommand();
        command.setAppName("开放接口应用");
        command.setIpWhitelistEnabled(0);
        command.setPayloadEncryptEnabled(1);
        command.setSignAlgorithm("HMAC_SHA256");
        command.setDemoApp(0);
        command.setStatus(1);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists payment_difference");
        jdbcTemplate.execute("drop table if exists payment_notification_record");
        jdbcTemplate.execute("drop table if exists payment_exception_order");
        jdbcTemplate.execute("drop table if exists payment_transaction_flow");
        jdbcTemplate.execute("drop table if exists payment_refund_order");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_business_order");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_application");
        createPaymentApplicationTable();
        createRelationTables();
    }

    private void createPaymentApplicationTable() {
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

    private void createRelationTables() {
        jdbcTemplate.execute("""
                create table payment_cashier_config (
                    id bigint primary key,
                    application_id bigint,
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_business_order (
                    id bigint primary key,
                    app_code varchar(128),
                    biz_order_no varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_order (
                    id bigint primary key,
                    business_order_id bigint,
                    pay_order_no varchar(128),
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_refund_order (
                    id bigint primary key,
                    payment_order_id bigint,
                    refund_order_no varchar(128),
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_transaction_flow (
                    id bigint primary key,
                    business_order_id bigint,
                    payment_order_id bigint,
                    refund_order_id bigint,
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_exception_order (
                    id bigint primary key,
                    related_order_no varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_notification_record (
                    id bigint primary key,
                    notification_no varchar(128),
                    related_order_no varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_difference (
                    id bigint primary key,
                    related_order_no varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
    }

    private void insertApplication(Long id, String appId) {
        jdbcTemplate.update("""
                        insert into payment_application (
                            id, app_id, app_name, secret_configured, secret_version, ip_whitelist_enabled,
                            payload_encrypt_enabled, demo_app, status, tenant_id, del_flag, created_at, updated_at
                        ) values (?, ?, '订单中心', 0, 0, 0, 0, 0, 1, 1, 0, current_timestamp, current_timestamp)
                        """,
                id, appId);
    }

    private void insertCashierConfig(Long id, Long applicationId, Long tenantId, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_cashier_config (id, application_id, tenant_id, del_flag)
                        values (?, ?, ?, ?)
                        """,
                id, applicationId, tenantId, delFlag);
    }

    private PaymentApplication singleApplication() {
        return applicationMapper.selectList(new LambdaQueryWrapper<PaymentApplication>()
                .orderByAsc(PaymentApplication::getCreatedAt)).get(0);
    }

    private Long countDeletedApplications() {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_application where id = 310001 and del_flag = 1",
                Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentApplicationMapper.class)
    @Import(PaymentApplicationServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentOperationAuditService paymentOperationAuditService() {
            return new TestPaymentOperationAuditService();
        }

        @Bean
        TestPaymentSensitiveValueService paymentSensitiveValueService() {
            return new TestPaymentSensitiveValueService();
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

    static class TestPaymentSensitiveValueService extends PaymentSensitiveValueService {

        private final List<String> encryptedPlaintexts = new ArrayList<>();

        TestPaymentSensitiveValueService() {
            super(null);
        }

        @Override
        public String encrypt(String plaintext) {
            encryptedPlaintexts.add(plaintext);
            return "enc:" + plaintext;
        }

        void clear() {
            encryptedPlaintexts.clear();
        }
    }
}
