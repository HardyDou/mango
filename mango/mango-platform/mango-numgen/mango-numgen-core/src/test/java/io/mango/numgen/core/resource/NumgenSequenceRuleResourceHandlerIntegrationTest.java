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
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NumgenSequenceRuleResourceHandler handler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void upsertCreatesGeneratorRuleAndSegments() {
        handler.upsert(sequenceRuleDeclaration("SO", "销售订单号", 1));

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("销售订单号");
        assertThat(stringValue("numgen_generator", "domain_code", "id = 100")).isEqualTo("ORDER");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 100")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "id = 110")).isEqualTo("ACTIVE");
        assertThat(count("numgen_rule_segment")).isEqualTo(3);
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("SO");
    }

    @Test
    void upsertUpdatesRuleAndReplacesSegments() {
        handler.upsert(sequenceRuleDeclaration("SO", "销售订单号", 1));

        handler.upsert(sequenceRuleDeclaration("PO", "采购订单号", 1));

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("采购订单号");
        assertThat(stringValue("numgen_rule", "rule_name", "id = 110")).isEqualTo("采购订单号默认规则");
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("PO");
        assertThat(intValue("numgen_rule_segment", "seq_width", "id = 113")).isEqualTo(6);
    }

    @Test
    void disableMarksGeneratorAndRuleDisabled() {
        ResourceDeclaration declaration = sequenceRuleDeclaration("SO", "销售订单号", 1);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(intValue("numgen_generator", "status", "id = 100")).isZero();
        assertThat(intValue("numgen_rule", "status", "id = 110")).isZero();
    }

    @Test
    void deletePhysicallyDeletesGeneratorRuleAndSegments() {
        ResourceDeclaration declaration = sequenceRuleDeclaration("SO", "销售订单号", 1);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("numgen_generator")).isZero();
        assertThat(count("numgen_rule")).isZero();
        assertThat(count("numgen_rule_segment")).isZero();
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

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists numgen_rule_segment");
        jdbcTemplate.execute("drop table if exists numgen_rule");
        jdbcTemplate.execute("drop table if exists numgen_generator");
        jdbcTemplate.execute("""
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
        jdbcTemplate.execute("""
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
        jdbcTemplate.execute("""
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

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                String.class);
    }

    private int intValue(String tableName, String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Integer.class);
        return value == null ? 0 : value;
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
