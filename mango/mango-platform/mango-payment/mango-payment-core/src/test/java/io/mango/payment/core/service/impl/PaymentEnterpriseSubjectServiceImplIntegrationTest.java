package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
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
        PaymentEnterpriseSubjectServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_enterprise_subject_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
public class PaymentEnterpriseSubjectServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentEnterpriseSubjectMapper subjectMapper;

    @Autowired
    private PaymentSubjectBankAccountMapper bankAccountMapper;

    @Autowired
    private PaymentEnterpriseSubjectServiceImpl service;

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
    void createEnterpriseSubjectEncryptsSensitiveFieldsAndCreatesDefaultBankAccountThroughRealMappers() {
        Long id = service.createEnterpriseSubject(command()).getData();

        PaymentEnterpriseSubject subject = subjectMapper.selectById(id);
        assertThat(subject.getTenantId()).isEqualTo(1L);
        assertThat(subject.getSubjectName()).isEqualTo("芒果科技有限公司");
        assertThat(subject.getCreditCodeHash()).isEqualTo("hash:91310000MA1PAY001X");
        assertThat(subject.getCreditCode()).isEqualTo("enc:91310000MA1PAY001X");
        assertThat(subject.getBankAccountNo()).isEqualTo("enc:6222000000000001");
        assertThat(subject.getBankName()).isEqualTo("招商银行");
        assertThat(subject.getStatus()).isEqualTo(1);

        PaymentSubjectBankAccountEntity account = singleBankAccount(id);
        assertThat(account.getTenantId()).isEqualTo(1L);
        assertThat(account.getAccountName()).isEqualTo("芒果科技有限公司");
        assertThat(account.getAccountNo()).isEqualTo("enc:6222000000000001");
        assertThat(account.getBankName()).isEqualTo("招商银行");
        assertThat(account.getAccountType()).isEqualTo("CORPORATE");
        assertThat(account.getDefaultAccount()).isEqualTo(1);
        assertThat(account.getStatus()).isEqualTo(1);
        assertThat(auditService.records).containsExactly(
                "CREATE_ENTERPRISE_SUBJECT|PAYMENT_ENTERPRISE_SUBJECT|" + id + "|SUCCESS");
        assertThat(sensitiveValueService.encryptedPlaintexts)
                .containsExactly("91310000MA1PAY001X", "6222000000000001");
    }

    @Test
    void detailEnterpriseSubjectMasksSensitiveValuesLoadedFromRealMapper() {
        insertSubject(320001L, "enc:credit-ciphertext", "enc:account-ciphertext", 0);

        PaymentEnterpriseSubjectVO result = service.detailEnterpriseSubject(320001L).getData();

        assertThat(result.getCreditCodeMask()).isEqualTo("mask:enc:credit-ciphertext:4:4");
        assertThat(result.getBankAccountNoMask()).isEqualTo("mask:enc:account-ciphertext:4:4");
    }

    @Test
    void updateEnterpriseSubjectUpdatesSubjectAndDefaultBankAccountThroughRealMappers() {
        insertSubject(320001L, "enc:old-credit", "enc:old-account", 0);
        insertBankAccount(330001L, 320001L, "enc:old-account", 0);
        SavePaymentEnterpriseSubjectCommand command = command();
        command.setId(320001L);
        command.setBankName("建设银行");

        service.updateEnterpriseSubject(command);

        PaymentEnterpriseSubject subject = subjectMapper.selectById(320001L);
        assertThat(subject.getCreditCode()).isEqualTo("enc:91310000MA1PAY001X");
        assertThat(subject.getCreditCodeHash()).isEqualTo("hash:91310000MA1PAY001X");
        assertThat(subject.getBankAccountNo()).isEqualTo("enc:6222000000000001");
        assertThat(subject.getBankName()).isEqualTo("建设银行");

        PaymentSubjectBankAccountEntity account = bankAccountMapper.selectById(330001L);
        assertThat(account.getAccountNo()).isEqualTo("enc:6222000000000001");
        assertThat(account.getBankName()).isEqualTo("建设银行");
        assertThat(bankAccountMapper.selectCount(null)).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "UPDATE_ENTERPRISE_SUBJECT|PAYMENT_ENTERPRISE_SUBJECT|320001|SUCCESS");
    }

    @Test
    void deleteEnterpriseSubjectRejectsWhenCashierConfigReferencesSubjectThroughRealMapperSql() {
        insertSubject(320001L, "enc:credit", "enc:account", 0);
        insertCashierConfig(350001L, "319999,320001", 0);

        assertThatThrownBy(() -> service.deleteEnterpriseSubject(320001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_DELETE_HAS_RELATIONS.getMessage());

        assertThat(subjectMapper.selectById(320001L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_ENTERPRISE_SUBJECT|PAYMENT_ENTERPRISE_SUBJECT|320001|REJECTED");
    }

    @Test
    void deleteEnterpriseSubjectLogicalDeletesWhenNoRelationsThroughRealMapperSql() {
        insertSubject(320001L, "enc:credit", "enc:account", 0);
        insertCashierConfig(350001L, "320001", 1);

        service.deleteEnterpriseSubject(320001L);

        assertThat(subjectMapper.selectById(320001L)).isNull();
        assertThat(countDeletedSubjects()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "DELETE_ENTERPRISE_SUBJECT|PAYMENT_ENTERPRISE_SUBJECT|320001|SUCCESS");
    }

    private SavePaymentEnterpriseSubjectCommand command() {
        SavePaymentEnterpriseSubjectCommand command = new SavePaymentEnterpriseSubjectCommand();
        command.setSubjectName("芒果科技有限公司");
        command.setCreditCode("91310000MA1PAY001X");
        command.setBankAccountNo("6222000000000001");
        command.setBankName("招商银行");
        command.setStatus(1);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("create alias if not exists find_in_set for \""
                + PaymentEnterpriseSubjectServiceImplIntegrationTest.class.getName() + ".findInSet\"");
        jdbcTemplate.execute("drop table if exists payment_settlement_summary");
        jdbcTemplate.execute("drop table if exists payment_difference");
        jdbcTemplate.execute("drop table if exists payment_notification_record");
        jdbcTemplate.execute("drop table if exists payment_exception_order");
        jdbcTemplate.execute("drop table if exists payment_transaction_flow");
        jdbcTemplate.execute("drop table if exists payment_refund_order");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_business_order");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_subject_bank_account");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        createSubjectTable();
        createBankAccountTable();
        createRelationTables();
    }

    private void createSubjectTable() {
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

    private void createBankAccountTable() {
        jdbcTemplate.execute("""
                create table payment_subject_bank_account (
                    id bigint primary key,
                    subject_id bigint,
                    account_name varchar(128),
                    account_no varchar(512),
                    bank_name varchar(128),
                    bank_branch_name varchar(128),
                    bank_code varchar(64),
                    account_type varchar(64),
                    default_account int,
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
                    enterprise_subject_ids varchar(512),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_contract (
                    id bigint primary key,
                    subject_id bigint,
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_business_order (
                    id bigint primary key,
                    subject_id bigint,
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
        jdbcTemplate.execute("""
                create table payment_settlement_summary (
                    id bigint primary key,
                    enterprise_subject_id bigint,
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
    }

    private void insertSubject(Long id, String creditCode, String accountNo, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_enterprise_subject (
                            id, subject_name, credit_code, credit_code_hash, bank_account_no, bank_name,
                            status, tenant_id, del_flag, created_at, updated_at
                        ) values (?, '芒果科技有限公司', ?, 'hash-old', ?, '招商银行',
                            1, 1, ?, current_timestamp, current_timestamp)
                        """,
                id, creditCode, accountNo, delFlag);
    }

    private void insertBankAccount(Long id, Long subjectId, String accountNo, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_subject_bank_account (
                            id, subject_id, account_name, account_no, bank_name, account_type,
                            default_account, status, tenant_id, del_flag, created_at, updated_at
                        ) values (?, ?, '芒果科技有限公司', ?, '招商银行', 'CORPORATE',
                            1, 1, 1, ?, current_timestamp, current_timestamp)
                        """,
                id, subjectId, accountNo, delFlag);
    }

    private void insertCashierConfig(Long id, String subjectIds, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_cashier_config (id, enterprise_subject_ids, tenant_id, del_flag)
                        values (?, ?, 1, ?)
                        """,
                id, subjectIds, delFlag);
    }

    private PaymentSubjectBankAccountEntity singleBankAccount(Long subjectId) {
        return bankAccountMapper.selectOne(new LambdaQueryWrapper<PaymentSubjectBankAccountEntity>()
                .eq(PaymentSubjectBankAccountEntity::getSubjectId, subjectId));
    }

    private Long countDeletedSubjects() {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_enterprise_subject where id = 320001 and del_flag = 1",
                Long.class);
    }

    public static boolean findInSet(Long value, String csv) {
        if (value == null || csv == null || csv.isBlank()) {
            return false;
        }
        String needle = value.toString();
        for (String item : csv.split(",")) {
            if (needle.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentEnterpriseSubjectMapper.class)
    @Import(PaymentEnterpriseSubjectServiceImpl.class)
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

        @Override
        public String stableHash(String plaintext) {
            return "hash:" + plaintext;
        }

        @Override
        public String mask(String storedValue, int prefixLength, int suffixLength) {
            return "mask:" + storedValue + ":" + prefixLength + ":" + suffixLength;
        }

        void clear() {
            encryptedPlaintexts.clear();
        }
    }
}
