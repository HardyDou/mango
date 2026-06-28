package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.core.entity.PaymentChannelCertificateRotationRecordEntity;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.mapper.PaymentChannelCertificateRotationRecordMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        PaymentChannelContractServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_channel_contract_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentChannelContractServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentChannelContractMapper contractMapper;

    @Autowired
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;

    @Autowired
    private PaymentChannelContractValueMapper contractValueMapper;

    @Autowired
    private PaymentChannelCertificateRotationRecordMapper rotationRecordMapper;

    @Autowired
    private PaymentChannelContractServiceImpl service;

    @Autowired
    private TestPaymentOperationAuditService auditService;

    @BeforeEach
    void setUp() {
        resetSchema();
        seedReferenceData();
        auditService.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createContractEncryptsSensitiveValuesAndPersistsCapabilityThroughRealMappers() {
        Long id = service.createChannelContract(contractCommand()).getData();

        PaymentChannelContract contract = contractMapper.selectById(id);
        assertThat(contract.getContractCode()).startsWith("ALLINPAY_320001_");
        assertThat(contract.getEnvironment()).isEqualTo("PROD");
        assertThat(contract.getConfigValuesJson()).contains("\"privateKey\":\"enc:ciphertext-value\"");
        assertThat(contract.getConfigValuesJson()).doesNotContain("private-key\"");
        PaymentChannelContractValueEntity secretValue = value(id, "privateKey");
        assertThat(secretValue.getEncryptedValue()).isEqualTo("ciphertext-value");
        assertThat(secretValue.getValueText()).isNull();
        assertThat(secretValue.getSensitiveFlag()).isEqualTo(1);
        PaymentChannelContractCapability capability = singleCapability(id);
        assertThat(capability.getChannelCapabilityId()).isEqualTo(332001L);
        assertThat(capability.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(capability.getMinAmount()).isEqualTo(10L);
        assertThat(auditService.records).containsExactly(
                "CREATE_CHANNEL_CONTRACT|PAYMENT_CHANNEL_CONTRACT|" + id + "|SUCCESS");

        String masked = service.detailChannelContract(id).getData().getConfigValuesJson();
        assertThat(masked).contains("\"privateKey\":\"******\"");
        assertThat(masked).doesNotContain("ciphertext-value");
    }

    @Test
    void createContractStoresFileValueAndRejectsFileAccessUrl() {
        Long id = service.createChannelContract(fileContractCommand("900001")).getData();

        PaymentChannelContractValueEntity fileValue = value(id, "certificateFileId");
        assertThat(fileValue.getFileId()).isEqualTo(900001L);
        assertThat(fileValue.getValueText()).isNull();
        assertThat(fileValue.getEncryptedValue()).isNull();
        assertThat(fileValue.getValueSource()).isEqualTo("FILE");

        SavePaymentChannelContractCommand invalid = fileContractCommand("https://files.example/cert.pem");
        assertThatThrownBy(() -> service.createChannelContract(invalid))
                .isInstanceOf(BizException.class)
                .hasMessage("证书文件只能保存文件 ID");
    }

    @Test
    void createOfflineCollectionContractDefaultsAccountFromSubject() {
        Long id = service.createChannelContract(offlineContractCommand()).getData();

        PaymentChannelContract stored = contractMapper.selectById(id);
        assertThat(stored.getConfigValuesJson()).contains("\"accountName\":\"芒果科技有限公司\"");
        assertThat(stored.getConfigValuesJson()).contains("\"accountNo\":\"enc:subject-account\"");
        assertThat(stored.getConfigValuesJson()).contains("\"bankName\":\"招商银行上海分行\"");
        PaymentChannelContractValueEntity accountValue = value(id, "accountNo");
        assertThat(accountValue.getEncryptedValue()).isEqualTo("subject-account");
        assertThat(accountValue.getValueText()).isNull();
        assertThat(accountValue.getSensitiveFlag()).isEqualTo(1);
    }

    @Test
    void updateContractReusesExistingCapabilityAndRejectsRemovingRoutedCapability() {
        insertContract(331002L, 330002L, "{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"privateKey\":\"enc:old\"}");
        insertContractCapability(331102L, 331002L, 332001L, "PERSONAL_WECHAT_QR", 10, null);
        SavePaymentChannelContractCommand update = contractCommand();
        update.setId(331002L);

        service.updateChannelContract(update);

        PaymentChannelContract updated = contractMapper.selectById(331002L);
        assertThat(updated.getContractCode()).isEqualTo("ALLINPAY_MANGO_TECH");
        assertThat(countCapabilities(331002L)).isEqualTo(1L);
        assertThat(singleCapability(331002L).getId()).isEqualTo(331102L);
        assertThat(singleCapability(331002L).getMinAmount()).isEqualTo(10L);

        jdbcTemplate.update("""
                insert into payment_method_route_rule_item
                    (id, rule_id, contract_capability_id, priority, weight, status, tenant_id, del_flag)
                values (335102, 334102, 331102, 10, 100, 1, 1, 0)
                """);
        SavePaymentChannelContractCommand remove = contractCommand();
        remove.setId(331002L);
        remove.setCapabilities(List.of());

        assertThatThrownBy(() -> service.updateChannelContract(remove))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("签约能力已被路由引用");
        assertThat(contractCapabilityMapper.selectById(331102L)).isNotNull();
    }

    @Test
    void deleteContractUsesRealRelationSqlAndDeletesChildrenWhenUnused() {
        insertContract(331002L, 330002L, "{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"privateKey\":\"enc:old\"}");
        insertContractValue(991001L, 331002L, "privateKey", null, "old", null, "CONFIG", 1);
        insertContractCapability(331102L, 331002L, 332001L, "PERSONAL_WECHAT_QR", 10, null);
        jdbcTemplate.update("""
                insert into payment_order
                    (id, contract_id, contract_capability_id, tenant_id)
                values (370001, 331002, 331102, 1)
                """);

        assertThatThrownBy(() -> service.deleteChannelContract(331002L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_CONTRACT_DELETE_HAS_RELATIONS.getMessage());
        assertThat(contractMapper.selectById(331002L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_CHANNEL_CONTRACT|PAYMENT_CHANNEL_CONTRACT|331002|REJECTED");

        auditService.clear();
        jdbcTemplate.update("delete from payment_order where id = 370001");
        service.deleteChannelContract(331002L);

        assertThat(contractMapper.selectById(331002L)).isNull();
        assertThat(countDeletedContracts(331002L)).isEqualTo(1L);
        assertThat(countCapabilitiesIncludingDeleted(331002L)).isZero();
        assertThat(countValuesIncludingDeleted(331002L)).isZero();
        assertThat(auditService.records).containsExactly(
                "DELETE_CHANNEL_CONTRACT|PAYMENT_CHANNEL_CONTRACT|331002|SUCCESS");
    }

    @Test
    void listAndRotateCertificateUseRealMapperSqlAndPersistRotationRecord() {
        insertContract(331002L, 330005L, "{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"certificateFileId\":\"900001\"}");
        insertContractValue(991001L, 331002L, "certificateFileId", null, null, 900001L, "FILE", 0);
        LocalDateTime oldExpireTime = LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.SECONDS);
        insertContractCapability(331102L, 331002L, 332001L, "PERSONAL_WECHAT_QR", 10, oldExpireTime);

        assertThat(service.listExpiringCertificates(7).getData())
                .extracting("contractCapabilityId")
                .containsExactly(331102L);

        LocalDateTime newExpireTime = LocalDateTime.now().plusDays(90).truncatedTo(ChronoUnit.SECONDS);
        RotatePaymentChannelContractCertificateCommand command = new RotatePaymentChannelContractCertificateCommand();
        command.setContractId(331002L);
        command.setContractCapabilityId(331102L);
        command.setCertificateFieldCode("certificateFileId");
        command.setNewCertificateFileId(900002L);
        command.setNewCertificateExpireTime(newExpireTime);
        command.setRotateReason("证书到期轮换");

        service.rotateCertificate(command);

        PaymentChannelContract contract = contractMapper.selectById(331002L);
        PaymentChannelContractCapability capability = contractCapabilityMapper.selectById(331102L);
        PaymentChannelContractValueEntity value = value(331002L, "certificateFileId");
        PaymentChannelCertificateRotationRecordEntity record = rotationRecordMapper.selectList(null).get(0);
        assertThat(contract.getConfigValuesJson()).contains("\"certificateFileId\":\"900002\"");
        assertThat(capability.getCertificateExpireTime()).isEqualTo(newExpireTime);
        assertThat(value.getFileId()).isEqualTo(900002L);
        assertThat(record.getOldCertificateFileId()).isEqualTo(900001L);
        assertThat(record.getNewCertificateFileId()).isEqualTo(900002L);
        assertThat(record.getOldCertificateExpireTime()).isEqualTo(oldExpireTime);
        assertThat(record.getNewCertificateExpireTime()).isEqualTo(newExpireTime);
        assertThat(record.getRotateReason()).isEqualTo("证书到期轮换");
        assertThat(auditService.records).containsExactly(
                "ROTATE_CHANNEL_CERTIFICATE|PAYMENT_CHANNEL_CONTRACT|331002|SUCCESS");
    }

    @Test
    void listExpiringCertificatesRejectsOutOfRangeDays() {
        assertThatThrownBy(() -> service.listExpiringCertificates(366))
                .isInstanceOf(BizException.class)
                .hasMessage("证书提醒天数必须在 0 到 365 之间");
    }

    private SavePaymentChannelContractCommand contractCommand() {
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("芒果科技通联签约");
        command.setSubjectId(320001L);
        command.setChannelId(330002L);
        command.setMerchantNo("ALLINPAY_MERCHANT_001");
        command.setConfigValuesJson("{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"privateKey\":\"private-key\"}");
        command.setCapabilities(List.of(capabilityCommand(332001L)));
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelContractCommand fileContractCommand(String fileValue) {
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("芒果科技证书签约");
        command.setSubjectId(320001L);
        command.setChannelId(330005L);
        command.setMerchantNo("CERT_MERCHANT_001");
        command.setConfigValuesJson("{\"merchantNo\":\"CERT_MERCHANT_001\",\"certificateFileId\":\"" + fileValue + "\"}");
        command.setCapabilities(List.of(capabilityCommand(332005L)));
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelContractCommand offlineContractCommand() {
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("芒果科技线下收款签约");
        command.setSubjectId(320001L);
        command.setChannelId(330004L);
        command.setMerchantNo("OFFLINE_COLLECTION_MERCHANT_001");
        command.setConfigValuesJson("{}");
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelContractCapabilityCommand capabilityCommand(Long channelCapabilityId) {
        SavePaymentChannelContractCapabilityCommand command = new SavePaymentChannelContractCapabilityCommand();
        command.setChannelCapabilityId(channelCapabilityId);
        command.setFeeRate(new BigDecimal("0.0060000000"));
        command.setMinAmount(10L);
        command.setMaxAmount(880000L);
        command.setPriority(10);
        command.setStatus(1);
        return command;
    }

    private PaymentChannelContractValueEntity value(Long contractId, String fieldCode) {
        return contractValueMapper.selectOne(new LambdaQueryWrapper<PaymentChannelContractValueEntity>()
                .eq(PaymentChannelContractValueEntity::getTenantId, 1L)
                .eq(PaymentChannelContractValueEntity::getContractId, contractId)
                .eq(PaymentChannelContractValueEntity::getFieldCode, fieldCode));
    }

    private PaymentChannelContractCapability singleCapability(Long contractId) {
        return contractCapabilityMapper.selectList(new LambdaQueryWrapper<PaymentChannelContractCapability>()
                .eq(PaymentChannelContractCapability::getTenantId, 1L)
                .eq(PaymentChannelContractCapability::getContractId, contractId)).get(0);
    }

    private Long countCapabilities(Long contractId) {
        return contractCapabilityMapper.selectCount(new LambdaQueryWrapper<PaymentChannelContractCapability>()
                .eq(PaymentChannelContractCapability::getTenantId, 1L)
                .eq(PaymentChannelContractCapability::getContractId, contractId));
    }

    private Long countDeletedContracts(Long contractId) {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_channel_contract where id = ? and del_flag = 1",
                Long.class,
                contractId);
    }

    private Long countCapabilitiesIncludingDeleted(Long contractId) {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_channel_contract_capability where contract_id = ?",
                Long.class,
                contractId);
    }

    private Long countValuesIncludingDeleted(Long contractId) {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_channel_contract_value where contract_id = ?",
                Long.class,
                contractId);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists payment_channel_certificate_rotation_record");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_value");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule_item");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_capability");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_channel_capability");
        jdbcTemplate.execute("drop table if exists payment_channel");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_enterprise_subject");
        createReferenceTables();
        createContractTables();
        createRelationTables();
    }

    private void createReferenceTables() {
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
                create table payment_method (
                    id bigint primary key,
                    method_code varchar(128),
                    method_name varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
    }

    private void createContractTables() {
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
                    config_values_json varchar(2048),
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
                    fee_rate decimal(18,10),
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
                create table payment_channel_contract_value (
                    id bigint primary key,
                    contract_id bigint,
                    field_code varchar(128),
                    value_text varchar(2048),
                    encrypted_value varchar(2048),
                    file_id bigint,
                    value_source varchar(64),
                    sensitive_flag int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_certificate_rotation_record (
                    id bigint primary key,
                    contract_id bigint,
                    contract_capability_id bigint,
                    certificate_field_code varchar(128),
                    old_certificate_file_id bigint,
                    new_certificate_file_id bigint,
                    old_certificate_expire_time timestamp,
                    new_certificate_expire_time timestamp,
                    rotate_reason varchar(512),
                    operator_id bigint,
                    operator_name varchar(128),
                    rotate_time timestamp,
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
                create table payment_method_route_rule_item (
                    id bigint primary key,
                    rule_id bigint,
                    contract_capability_id bigint,
                    priority int,
                    weight int,
                    status int,
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_order (
                    id bigint primary key,
                    contract_id bigint,
                    contract_capability_id bigint,
                    tenant_id bigint
                )
                """);
    }

    private void seedReferenceData() {
        jdbcTemplate.update("""
                insert into payment_enterprise_subject
                    (id, subject_name, bank_account_no, bank_name, status, tenant_id, del_flag)
                values (320001, '芒果科技有限公司', 'enc:subject-account', '招商银行上海分行', 1, 1, 0)
                """);
        insertChannel(330002L, "ALLINPAY", "通联支付通道", "PROD", """
                [
                  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2}
                ]
                """);
        insertChannel(330005L, "ALLINPAY", "通联证书通道", "PROD", """
                [
                  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"certificateFileId","label":"证书文件","component":"fileId","dataType":"fileId","required":true,"sort":2}
                ]
                """);
        insertChannel(330004L, "OFFLINE_COLLECTION", "线下收款", "OFFLINE_COLLECTION", """
                [
                  {"name":"accountName","label":"收款户名","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"accountNo","label":"收款账号","component":"input","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2},
                  {"name":"bankName","label":"开户行","component":"input","dataType":"string","required":true,"sort":3}
                ]
                """);
        insertChannelCapability(332001L, 330002L, "PERSONAL_WECHAT_QR", "WEB");
        insertChannelCapability(332005L, 330005L, "PERSONAL_WECHAT_QR", "WEB");
        jdbcTemplate.update("""
                insert into payment_method (id, method_code, method_name, tenant_id, del_flag)
                values (340001, 'PERSONAL_WECHAT_QR', '微信扫码', 1, 0)
                """);
    }

    private void insertChannel(Long id, String code, String name, String environment, String templateJson) {
        jdbcTemplate.update("""
                insert into payment_channel (
                    id, channel_code, channel_name, environment, channel_type, adapter_type,
                    field_template_json, status, tenant_id, del_flag
                ) values (?, ?, ?, ?, 'AGGREGATOR', ?, ?, 1, 1, 0)
                """, id, code, name, environment, code, templateJson);
    }

    private void insertChannelCapability(Long id, Long channelId, String methodCode, String terminalType) {
        jdbcTemplate.update("""
                insert into payment_channel_capability (
                    id, channel_id, method_code, terminal_type, environment, supports_refund, supports_query,
                    supports_close, supports_bill, supports_reconcile, min_amount, max_amount, status, tenant_id, del_flag
                ) values (?, ?, ?, ?, 'PROD', 1, 1, 1, 1, 1, 1, 999999, 1, 1, 0)
                """, id, channelId, methodCode, terminalType);
    }

    private void insertContract(Long id, Long channelId, String configValuesJson) {
        jdbcTemplate.update("""
                insert into payment_channel_contract (
                    id, contract_code, contract_name, subject_id, channel_id, environment, merchant_no,
                    config_values_json, status, tenant_id, del_flag
                ) values (?, 'ALLINPAY_MANGO_TECH', '芒果科技通联签约', 320001, ?, 'PROD',
                    'ALLINPAY_MERCHANT_001', ?, 1, 1, 0)
                """, id, channelId, configValuesJson);
    }

    private void insertContractCapability(
            Long id,
            Long contractId,
            Long channelCapabilityId,
            String methodCode,
            Integer priority,
            LocalDateTime certificateExpireTime) {
        jdbcTemplate.update("""
                insert into payment_channel_contract_capability (
                    id, contract_id, channel_capability_id, method_code, terminal_type, fee_rate,
                    min_amount, max_amount, priority, certificate_expire_time, status, tenant_id, del_flag
                ) values (?, ?, ?, ?, 'WEB', 0.0060000000, 10, 880000, ?, ?, 1, 1, 0)
                """, id, contractId, channelCapabilityId, methodCode, priority, certificateExpireTime);
    }

    private void insertContractValue(
            Long id,
            Long contractId,
            String fieldCode,
            String valueText,
            String encryptedValue,
            Long fileId,
            String valueSource,
            Integer sensitiveFlag) {
        jdbcTemplate.update("""
                insert into payment_channel_contract_value (
                    id, contract_id, field_code, value_text, encrypted_value, file_id,
                    value_source, sensitive_flag, tenant_id, del_flag
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)
                """, id, contractId, fieldCode, valueText, encryptedValue, fileId, valueSource, sensitiveFlag);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentChannelContractMapper.class)
    @Import(PaymentChannelContractServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentOperationAuditService paymentOperationAuditService() {
            return new TestPaymentOperationAuditService();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ICryptoService cryptoService() {
            return new ICryptoService() {
                @Override
                public String encrypt(String plainText) {
                    if ("private-key".equals(plainText)) {
                        return "ciphertext-value";
                    }
                    return plainText;
                }

                @Override
                public String encrypt(String plaintext, String iv) {
                    return encrypt(plaintext);
                }

                @Override
                public String decrypt(String cipherText) {
                    return cipherText;
                }

                @Override
                public String decrypt(String ciphertext, String iv) {
                    return decrypt(ciphertext);
                }
            };
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
