package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentChannelCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelCapability;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
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
        PaymentChannelServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_channel_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class PaymentChannelServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentChannelMapper channelMapper;

    @Autowired
    private PaymentChannelCapabilityMapper capabilityMapper;

    @Autowired
    private PaymentChannelServiceImpl service;

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
    void createChannelPersistsChannelAndCapabilityThroughRealMappers() {
        Long id = service.createChannel(command()).getData();

        PaymentChannel channel = channelMapper.selectById(id);
        assertThat(channel.getTenantId()).isEqualTo(1L);
        assertThat(channel.getChannelCode()).isEqualTo("LIANLIAN_PAY");
        assertThat(channel.getEnvironment()).isEqualTo("PROD");
        assertThat(channel.getBillFetchModes()).isEqualTo("MANUAL,FTP");
        PaymentChannelCapability capability = singleCapability(id);
        assertThat(capability.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(capability.getTerminalType()).isEqualTo("WEB");
        assertThat(capability.getEnvironment()).isEqualTo("PROD");
        assertThat(capability.getMinAmount()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "CREATE_CHANNEL|PAYMENT_CHANNEL|LIANLIAN_PAY|SUCCESS");
    }

    @Test
    void createChannelRejectsInvalidSensitiveFieldTemplateBeforeInsert() {
        SavePaymentChannelCommand command = command();
        command.setFieldTemplateJson("""
                [{"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","sensitive":true}]
                """);

        assertThatThrownBy(() -> service.createChannel(command))
                .isInstanceOf(BizException.class)
                .hasMessage("商户私钥敏感字段必须声明加密或脱敏");

        assertThat(channelMapper.selectCount(null)).isZero();
        assertThat(capabilityMapper.selectCount(null)).isZero();
    }

    @Test
    void updateChannelRejectsRemovingReferencedCapabilityThroughRealMapperSql() {
        insertChannel(330009L, "LIANLIAN_PAY", "AGGREGATOR");
        insertCapability(332009L, 330009L);
        jdbcTemplate.update("""
                insert into payment_channel_contract_capability
                    (id, channel_capability_id, tenant_id, del_flag)
                values (333009, 332009, 1, 0)
                """);
        SavePaymentChannelCommand command = command();
        command.setId(330009L);
        command.setCapabilities(List.of());

        assertThatThrownBy(() -> service.updateChannel(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("通道能力存在签约或路由引用");

        assertThat(capabilityMapper.selectById(332009L)).isNotNull();
    }

    @Test
    void deleteChannelRejectsWhenContractReferencesChannelThroughRealMapperSql() {
        insertChannel(330009L, "LIANLIAN_PAY", "AGGREGATOR");
        jdbcTemplate.update("""
                insert into payment_channel_contract (id, channel_id, tenant_id, del_flag)
                values (331009, 330009, 1, 0)
                """);

        assertThatThrownBy(() -> service.deleteChannel(330009L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_DELETE_HAS_RELATIONS.getMessage());

        assertThat(channelMapper.selectById(330009L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_CHANNEL|PAYMENT_CHANNEL|LIANLIAN_PAY|REJECTED");
    }

    @Test
    void deleteChannelDeletesChildCapabilitiesAndLogicalDeletesChannelWhenUnused() {
        insertChannel(330009L, "LIANLIAN_PAY", "AGGREGATOR");
        insertCapability(332009L, 330009L);

        service.deleteChannel(330009L);

        assertThat(capabilityMapper.selectById(332009L)).isNull();
        assertThat(channelMapper.selectById(330009L)).isNull();
        assertThat(countDeletedChannels()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "DELETE_CHANNEL|PAYMENT_CHANNEL|LIANLIAN_PAY|SUCCESS");
    }

    private SavePaymentChannelCommand command() {
        SavePaymentChannelCommand command = new SavePaymentChannelCommand();
        command.setChannelCode(PaymentChannelCode.LIANLIAN_PAY);
        command.setChannelName("连连支付通道");
        command.setChannelType("AGGREGATOR");
        command.setAdapterType("LIANLIAN_PAY");
        command.setGatewayBaseUrl("https://openapi.lianlianpay.com");
        command.setFieldTemplateJson("""
                [{"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true}]
                """);
        command.setCapabilitySummary("微信扫码、退款、查单、账单、对账");
        command.setBillFetchModes(List.of("MANUAL", "FTP"));
        command.setCapabilities(List.of(capabilityCommand()));
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelCapabilityCommand capabilityCommand() {
        SavePaymentChannelCapabilityCommand command = new SavePaymentChannelCapabilityCommand();
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setSupportsRefund(1);
        command.setSupportsQuery(1);
        command.setSupportsClose(1);
        command.setSupportsBill(1);
        command.setSupportsReconcile(1);
        command.setMinAmount(1L);
        command.setMaxAmount(999999L);
        command.setStatus(1);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists payment_method_route_rule_item");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_capability");
        jdbcTemplate.execute("drop table if exists payment_settlement_summary");
        jdbcTemplate.execute("drop table if exists payment_reconciliation");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_channel_contract");
        jdbcTemplate.execute("drop table if exists payment_channel_capability");
        jdbcTemplate.execute("drop table if exists payment_channel");
        createChannelTable();
        createCapabilityTable();
        createRelationTables();
    }

    private void createChannelTable() {
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
    }

    private void createCapabilityTable() {
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
    }

    private void createRelationTables() {
        jdbcTemplate.execute("create table payment_channel_contract (id bigint primary key, channel_id bigint, tenant_id bigint, del_flag int default 0)");
        jdbcTemplate.execute("create table payment_order (id bigint primary key, channel_id bigint, tenant_id bigint)");
        jdbcTemplate.execute("create table payment_reconciliation (id bigint primary key, channel_code varchar(128), tenant_id bigint, del_flag int default 0)");
        jdbcTemplate.execute("create table payment_settlement_summary (id bigint primary key, channel_code varchar(128), tenant_id bigint, del_flag int default 0)");
        jdbcTemplate.execute("create table payment_channel_contract_capability (id bigint primary key, channel_capability_id bigint, tenant_id bigint, del_flag int default 0)");
        jdbcTemplate.execute("create table payment_method_route_rule_item (id bigint primary key, contract_capability_id bigint, tenant_id bigint)");
    }

    private void insertChannel(Long id, String channelCode, String channelType) {
        jdbcTemplate.update("""
                        insert into payment_channel (
                            id, channel_code, channel_name, environment, channel_type, adapter_type, status,
                            tenant_id, del_flag, created_at, updated_at
                        ) values (?, ?, '连连支付通道', 'PROD', ?, 'LIANLIAN_PAY', 1, 1, 0, current_timestamp, current_timestamp)
                        """,
                id, channelCode, channelType);
    }

    private void insertCapability(Long id, Long channelId) {
        jdbcTemplate.update("""
                        insert into payment_channel_capability (
                            id, channel_id, method_code, terminal_type, environment, supports_refund, supports_query,
                            supports_close, supports_bill, supports_reconcile, min_amount, max_amount, status,
                            tenant_id, del_flag, created_at, updated_at
                        ) values (?, ?, 'PERSONAL_WECHAT_QR', 'WEB', 'PROD', 1, 1, 1, 1, 1, 1, 999999, 1, 1, 0,
                            current_timestamp, current_timestamp)
                        """,
                id, channelId);
    }

    private PaymentChannelCapability singleCapability(Long channelId) {
        return capabilityMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentChannelCapability>()
                .eq(PaymentChannelCapability::getChannelId, channelId)).get(0);
    }

    private Long countDeletedChannels() {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_channel where id = 330009 and del_flag = 1",
                Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentChannelMapper.class)
    @Import(PaymentChannelServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentOperationAuditService paymentOperationAuditService() {
            return new TestPaymentOperationAuditService();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
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
