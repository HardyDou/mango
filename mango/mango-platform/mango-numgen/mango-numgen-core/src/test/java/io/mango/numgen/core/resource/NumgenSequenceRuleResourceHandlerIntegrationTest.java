package io.mango.numgen.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NumgenSequenceRuleResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:numgen_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/numgen/*.xml"
})
class NumgenSequenceRuleResourceHandlerIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private NumgenSequenceRuleResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        rebuildTables();
    }

    @Test
    void upsertCreatesGeneratorRuleAndSegments() throws Exception {
        handler.upsert(sequenceRuleDeclaration("SO", "销售订单号", 1));

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("销售订单号");
        assertThat(stringValue("numgen_generator", "domain_code", "id = 100")).isEqualTo("ORDER");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 100")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "id = 110")).isEqualTo("ACTIVE");
        assertThat(count("numgen_rule_segment")).isEqualTo(3);
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("SO");
    }

    @Test
    void upsertUpdatesRuleAndReplacesSegments() throws Exception {
        handler.upsert(sequenceRuleDeclaration("SO", "销售订单号", 1));

        handler.upsert(sequenceRuleDeclaration("PO", "采购订单号", 1));

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("采购订单号");
        assertThat(stringValue("numgen_rule", "rule_name", "id = 110")).isEqualTo("采购订单号默认规则");
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("PO");
        assertThat(intValue("numgen_rule_segment", "seq_width", "id = 113")).isEqualTo(6);
    }

    @Test
    void disableMarksGeneratorAndRuleDisabled() throws Exception {
        ResourceDeclaration declaration = sequenceRuleDeclaration("SO", "销售订单号", 1);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(intValue("numgen_generator", "status", "id = 100")).isZero();
        assertThat(intValue("numgen_rule", "status", "id = 110")).isZero();
    }

    @Test
    void deletePhysicallyDeletesGeneratorRuleAndSegments() throws Exception {
        ResourceDeclaration declaration = sequenceRuleDeclaration("SO", "销售订单号", 1);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("numgen_generator")).isZero();
        assertThat(count("numgen_rule")).isZero();
        assertThat(count("numgen_rule_segment")).isZero();
    }

    @Test
    void paymentSequenceRulesCreateExpectedGeneratorsRulesAndSegments() throws Exception {
        List<ResourceDeclaration> declarations = paymentSequenceRuleDeclarations();
        for (ResourceDeclaration declaration : declarations) {
            handler.upsert(declaration);
        }

        assertThat(declarations).hasSize(20);
        assertThat(count("numgen_generator")).isEqualTo(20);
        assertThat(count("numgen_rule")).isEqualTo(20);
        assertThat(count("numgen_rule_segment")).isEqualTo(60);
        assertThat(stringValue("numgen_generator", "gen_key", "id = 900000000001"))
                .isEqualTo("PAY_BIZ_ORDER_NO");
        assertThat(stringValue("numgen_generator", "domain_code", "id = 900000000001"))
                .isEqualTo("PAYMENT");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 900000000001")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "id = 900000010001"))
                .isEqualTo("ACTIVE");
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 900000020011"))
                .isEqualTo("BO");
        assertThat(intValue("numgen_rule_segment", "sequence_scope", "id = 900000020012")).isOne();
        assertThat(intValue("numgen_rule_segment", "seq_width", "id = 900000020013")).isEqualTo(8);
    }

    private List<ResourceDeclaration> paymentSequenceRuleDeclarations() {
        return List.of(
                paymentSequenceRule(900000000001L, 900000010001L, 900000020011L,
                        "payment.numgen.biz-order-no", "PAY_BIZ_ORDER_NO", "支付业务单号", "BO"),
                paymentSequenceRule(900000000002L, 900000010002L, 900000020021L,
                        "payment.numgen.pay-order-no", "PAY_ORDER_NO", "支付订单号", "PO"),
                paymentSequenceRule(900000000003L, 900000010003L, 900000020031L,
                        "payment.numgen.refund-order-no", "PAY_REFUND_ORDER_NO", "退款订单号", "RO"),
                paymentSequenceRule(900000000004L, 900000010004L, 900000020041L,
                        "payment.numgen.settle-order-no", "PAY_SETTLE_ORDER_NO", "结算订单号", "SO"),
                paymentSequenceRule(900000000005L, 900000010005L, 900000020051L,
                        "payment.numgen.bill-order-no", "PAY_BILL_ORDER_NO", "账单单号", "BI"),
                paymentSequenceRule(900000000006L, 900000010006L, 900000020061L,
                        "payment.numgen.reconcile-order-no", "PAY_RECONCILE_ORDER_NO", "对账单号", "RC"),
                paymentSequenceRule(900000000007L, 900000010007L, 900000020071L,
                        "payment.numgen.merchant-order-no", "PAY_MERCHANT_ORDER_NO", "商户单号", "MO"),
                paymentSequenceRule(900000000008L, 900000010008L, 900000020081L,
                        "payment.numgen.channel-order-no", "PAY_CHANNEL_ORDER_NO", "渠道单号", "CO"),
                paymentSequenceRule(900000000009L, 900000010009L, 900000020091L,
                        "payment.numgen.withdraw-order-no", "PAY_WITHDRAW_ORDER_NO", "提现单号", "WO"),
                paymentSequenceRule(900000000010L, 900000010010L, 900000020101L,
                        "payment.numgen.transfer-order-no", "PAY_TRANSFER_ORDER_NO", "转账单号", "TO"),
                paymentSequenceRule(900000000011L, 900000010011L, 900000020111L,
                        "payment.numgen.invoice-order-no", "PAY_INVOICE_ORDER_NO", "发票单号", "IO"),
                paymentSequenceRule(900000000012L, 900000010012L, 900000020121L,
                        "payment.numgen.account-flow-no", "PAY_ACCOUNT_FLOW_NO", "账户流水号", "AF"),
                paymentSequenceRule(900000000013L, 900000010013L, 900000020131L,
                        "payment.numgen.fee-order-no", "PAY_FEE_ORDER_NO", "费用单号", "FE"),
                paymentSequenceRule(900000000014L, 900000010014L, 900000020141L,
                        "payment.numgen.adjust-order-no", "PAY_ADJUST_ORDER_NO", "调账单号", "AD"),
                paymentSequenceRule(900000000015L, 900000010015L, 900000020151L,
                        "payment.numgen.freeze-order-no", "PAY_FREEZE_ORDER_NO", "冻结单号", "FR"),
                paymentSequenceRule(900000000016L, 900000010016L, 900000020161L,
                        "payment.numgen.unfreeze-order-no", "PAY_UNFREEZE_ORDER_NO", "解冻单号", "UF"),
                paymentSequenceRule(900000000017L, 900000010017L, 900000020171L,
                        "payment.numgen.chargeback-order-no", "PAY_CHARGEBACK_ORDER_NO", "拒付单号", "CB"),
                paymentSequenceRule(900000000018L, 900000010018L, 900000020181L,
                        "payment.numgen.compensate-order-no", "PAY_COMPENSATE_ORDER_NO", "补偿单号", "CP"),
                paymentSequenceRule(900000000019L, 900000010019L, 900000020191L,
                        "payment.numgen.batch-order-no", "PAY_BATCH_ORDER_NO", "批次单号", "BA"),
                paymentSequenceRule(900000000020L, 900000010020L, 900000020201L,
                        "payment.numgen.audit-order-no", "PAY_AUDIT_ORDER_NO", "审核单号", "AU")
        );
    }

    private ResourceDeclaration paymentSequenceRule(Long generatorId, Long ruleId, Long firstSegmentId,
                                                    String bizKey, String genKey, String name, String prefix) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(String.valueOf(2026061800600000000L + generatorId));
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.SEQUENCE_RULE);
        declaration.setModuleCode("payment");
        declaration.setModuleName("支付中心");
        declaration.setBizKey(bizKey);
        declaration.setName(name);
        declaration.setTargetModule("numgen");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "generatorId", ResourceFieldType.LONG, generatorId);
        field(declaration, "ruleId", ResourceFieldType.LONG, ruleId);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "genKey", ResourceFieldType.STRING, genKey);
        field(declaration, "genName", ResourceFieldType.STRING, name);
        field(declaration, "domainCode", ResourceFieldType.STRING, "PAYMENT");
        field(declaration, "ruleName", ResourceFieldType.STRING, name + "默认规则");
        field(declaration, "ruleVersion", ResourceFieldType.INT, 1);
        field(declaration, "status", ResourceFieldType.INT, 1);
        field(declaration, "publishStatus", ResourceFieldType.INT, 1);
        field(declaration, "versionState", ResourceFieldType.STRING, "ACTIVE");
        field(declaration, "segments", ResourceFieldType.LIST, List.of(
                Map.of(
                        "id", firstSegmentId,
                        "sortOrder", 1,
                        "segmentType", "TEXT",
                        "segmentName", "业务前缀",
                        "literalValue", prefix,
                        "padChar", "0",
                        "sequenceScope", 0
                ),
                Map.of(
                        "id", firstSegmentId + 1,
                        "sortOrder", 2,
                        "segmentType", "DATE",
                        "segmentName", "日期",
                        "dateFormat", "yyyyMMdd",
                        "padChar", "0",
                        "sequenceScope", 1
                ),
                Map.of(
                        "id", firstSegmentId + 2,
                        "sortOrder", 3,
                        "segmentType", "SEQ",
                        "segmentName", "日内序号",
                        "seqWidth", 8,
                        "padChar", "0",
                        "sequenceScope", 0
                )
        ));
        return declaration;
    }

    private ResourceDeclaration sequenceRuleDeclaration(String prefix, String name, int ruleVersion) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800600000100");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.SEQUENCE_RULE);
        declaration.setModuleCode("order");
        declaration.setBizKey("order.numgen.sales-order-no");
        declaration.setName(name);
        declaration.setTargetModule("numgen");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "generatorId", ResourceFieldType.LONG, 100L);
        field(declaration, "ruleId", ResourceFieldType.LONG, 110L);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "genKey", ResourceFieldType.STRING, "ORDER_NO");
        field(declaration, "genName", ResourceFieldType.STRING, name);
        field(declaration, "domainCode", ResourceFieldType.STRING, "ORDER");
        field(declaration, "ruleName", ResourceFieldType.STRING, name + "默认规则");
        field(declaration, "ruleVersion", ResourceFieldType.INT, ruleVersion);
        field(declaration, "status", ResourceFieldType.INT, 1);
        field(declaration, "publishStatus", ResourceFieldType.INT, 1);
        field(declaration, "versionState", ResourceFieldType.STRING, "ACTIVE");
        field(declaration, "segments", ResourceFieldType.LIST, List.of(
                Map.of(
                        "id", 111L,
                        "sortOrder", 1,
                        "segmentType", "TEXT",
                        "segmentName", "业务前缀",
                        "literalValue", prefix,
                        "padChar", "0",
                        "sequenceScope", 0
                ),
                Map.of(
                        "id", 112L,
                        "sortOrder", 2,
                        "segmentType", "DATE",
                        "segmentName", "日期",
                        "dateFormat", "yyyyMMdd",
                        "padChar", "0",
                        "sequenceScope", 1
                ),
                Map.of(
                        "id", 113L,
                        "sortOrder", 3,
                        "segmentType", "SEQ",
                        "segmentName", "日内序号",
                        "seqWidth", "SO".equals(prefix) ? 4 : 6,
                        "padChar", "0",
                        "sequenceScope", 0
                )
        ));
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists numgen_rule_segment");
        execute("drop table if exists numgen_rule");
        execute("drop table if exists numgen_generator");
        execute("""
                create table numgen_generator (
                    id bigint not null,
                    gen_key varchar(128) not null,
                    gen_name varchar(128) not null,
                    domain_code varchar(64) not null default 'NUMGEN',
                    status tinyint not null default 1,
                    current_rule_version int,
                    current_publish_status tinyint not null default 0,
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    del_flag tinyint not null default 0,
                    primary key (id),
                    unique key uk_numgen_generator_tenant_key (tenant_id, gen_key, del_flag)
                )
                """);
        execute("""
                create table numgen_rule (
                    id bigint not null,
                    gen_key varchar(128) not null,
                    rule_name varchar(128) not null,
                    version int not null default 1,
                    status tinyint not null default 1,
                    publish_status tinyint not null default 0,
                    version_state varchar(16) not null default 'DRAFT',
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    del_flag tinyint not null default 0,
                    primary key (id),
                    unique key uk_numgen_rule_tenant_key_version (tenant_id, gen_key, version, del_flag)
                )
                """);
        execute("""
                create table numgen_rule_segment (
                    id bigint not null,
                    rule_id bigint not null,
                    sort_order int not null,
                    segment_type varchar(32) not null,
                    segment_name varchar(128) not null,
                    literal_value varchar(128),
                    variable_key varchar(128),
                    date_format varchar(64),
                    seq_width int,
                    pad_char varchar(1) default '0',
                    sequence_scope tinyint not null default 0,
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    primary key (id)
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Configuration
    @Import(NumgenSequenceRuleResourceHandler.class)
    @MapperScan(basePackageClasses = {
            NumgenGeneratorMapper.class,
            NumgenRuleMapper.class,
            NumgenRuleSegmentMapper.class
    })
    static class TestConfig {
    }
}
