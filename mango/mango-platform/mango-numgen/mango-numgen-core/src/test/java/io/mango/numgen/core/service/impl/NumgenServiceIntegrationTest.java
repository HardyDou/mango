package io.mango.numgen.core.service.impl;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.core.service.INumgenGeneratorService;
import io.mango.numgen.core.service.INumgenRuleService;
import io.mango.numgen.core.service.INumgenSegmentService;
import io.mango.numgen.core.service.INumgenService;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        NumgenServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:numgen;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class NumgenServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private INumgenGeneratorService generatorService;

    @Autowired
    private INumgenRuleService ruleService;

    @Autowired
    private INumgenSegmentService segmentService;

    @Autowired
    private INumgenService numgenService;

    @Autowired
    private NumgenSequenceAllocator sequenceAllocator;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        rebuildTables();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void publishLatestDraft_updatesEffectiveVersionAndClearsUnsyncedState() {
        seedPublishedAndDraftRules();

        NumgenGeneratorVO beforePublish = firstGenerator();
        assertThat(beforePublish.getCurrentRuleVersion()).isEqualTo(1);
        assertThat(beforePublish.getCurrentPublishStatus()).isEqualTo(1);
        assertThat(beforePublish.getHasUnpublishedChanges()).isTrue();

        NumgenPublishCommand command = new NumgenPublishCommand();
        command.setGenKey("ORDER_NO");
        assertThat(ruleService.publishRule(command).getData()).isTrue();

        assertThat(intValue("numgen_generator", "current_rule_version", "gen_key = 'ORDER_NO'")).isEqualTo(2);
        assertThat(intValue("numgen_generator", "current_publish_status", "gen_key = 'ORDER_NO'")).isEqualTo(1);
        assertThat(intValue("numgen_rule", "publish_status", "gen_key = 'ORDER_NO' AND version = 1")).isEqualTo(0);
        assertThat(intValue("numgen_rule", "publish_status", "gen_key = 'ORDER_NO' AND version = 2")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "gen_key = 'ORDER_NO' AND version = 1")).isEqualTo("HISTORY");
        assertThat(stringValue("numgen_rule", "version_state", "gen_key = 'ORDER_NO' AND version = 2")).isEqualTo("ACTIVE");

        NumgenGeneratorVO afterPublish = firstGenerator();
        assertThat(afterPublish.getCurrentRuleVersion()).isEqualTo(2);
        assertThat(afterPublish.getCurrentPublishStatus()).isEqualTo(1);
        assertThat(afterPublish.getHasUnpublishedChanges()).isFalse();

        assertThatThrownBy(() -> ruleService.publishRule(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("没有可发布的规则");
    }

    @Test
    void updateGenerator_keepsEffectiveVersionFields() {
        seedPublishedAndDraftRules();

        SaveNumgenGeneratorCommand command = new SaveNumgenGeneratorCommand();
        command.setId(1L);
        command.setGenKey("ORDER_NO");
        command.setGenName("订单号规则新版");
        command.setStatus(1);

        assertThat(generatorService.updateGenerator(command).getData()).isTrue();

        assertThat(stringValue("numgen_generator", "gen_name", "id = 1")).isEqualTo("订单号规则新版");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 1")).isEqualTo(1);
        assertThat(intValue("numgen_generator", "current_publish_status", "id = 1")).isEqualTo(1);
    }

    @Test
    void updateGenerator_rejectsGenKeyRename() {
        seedPublishedAndDraftRules();

        SaveNumgenGeneratorCommand command = new SaveNumgenGeneratorCommand();
        command.setId(1L);
        command.setGenKey("ORDER_NO_NEW");
        command.setGenName("订单号规则新版");
        command.setStatus(1);

        assertThatThrownBy(() -> generatorService.updateGenerator(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("业务 Key 不允许修改");
    }

    @Test
    void publishedRuleSegmentsCannotBeChanged() {
        seedPublishedAndDraftRules();

        SaveNumgenRuleSegmentCommand command = new SaveNumgenRuleSegmentCommand();
        command.setRuleId(101L);
        command.setSortOrder(3);
        command.setSegmentType("TEXT");
        command.setSegmentName("后缀");
        command.setLiteralValue("END");

        assertThatThrownBy(() -> segmentService.createSegment(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只有草稿版本可以修改片段");

        assertThatThrownBy(() -> segmentService.deleteSegment(1001L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只有草稿版本可以修改片段");
    }

    @Test
    void updateSegment_checksCurrentOwnerRuleAndRejectsRuleReassignment() {
        seedPublishedAndDraftRules();

        SaveNumgenRuleSegmentCommand command = new SaveNumgenRuleSegmentCommand();
        command.setId(1001L);
        command.setRuleId(102L);
        command.setSortOrder(1);
        command.setSegmentType("TEXT");
        command.setSegmentName("固定前缀");
        command.setLiteralValue("XX");

        assertThatThrownBy(() -> segmentService.updateSegment(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只有草稿版本可以修改片段");

        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 1001")).isEqualTo("SO");
        assertThat(longValue("numgen_rule_segment", "rule_id", "id = 1001")).isEqualTo(101L);
    }

    @Test
    void publishHistoricalVersion_clonesItAsNextActiveVersionAndKeepsSequenceHistory() {
        seedPublishedAndDraftRules();
        NumgenNextCommand next = new NumgenNextCommand();
        next.setGenKey("ORDER_NO");
        assertThat(numgenService.nextValue(next)).isEqualTo("SO0001");
        assertThat(numgenService.nextValue(next)).isEqualTo("SO0002");

        NumgenPublishCommand publishDraft = new NumgenPublishCommand();
        publishDraft.setGenKey("ORDER_NO");
        assertThat(ruleService.publishRule(publishDraft).getData()).isTrue();

        NumgenPublishCommand rollback = new NumgenPublishCommand();
        rollback.setRuleId(101L);
        assertThat(ruleService.publishRule(rollback).getData()).isTrue();

        assertThat(intValue("numgen_generator", "current_rule_version", "gen_key = 'ORDER_NO'")).isEqualTo(3);
        assertThat(intValue("numgen_rule", "publish_status", "gen_key = 'ORDER_NO' AND version = 1")).isEqualTo(0);
        assertThat(intValue("numgen_rule", "publish_status", "gen_key = 'ORDER_NO' AND version = 2")).isEqualTo(0);
        assertThat(intValue("numgen_rule", "publish_status", "gen_key = 'ORDER_NO' AND version = 3")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "gen_key = 'ORDER_NO' AND version = 1")).isEqualTo("HISTORY");
        assertThat(stringValue("numgen_rule", "version_state", "gen_key = 'ORDER_NO' AND version = 2")).isEqualTo("HISTORY");
        assertThat(stringValue("numgen_rule", "version_state", "gen_key = 'ORDER_NO' AND version = 3")).isEqualTo("ACTIVE");
        assertThat(intValue("numgen_rule_segment", "COUNT(*)", "rule_id = (SELECT id FROM numgen_rule WHERE gen_key = 'ORDER_NO' AND version = 3)")).isEqualTo(2);

        NumgenGeneratorVO afterRollback = firstGenerator();
        assertThat(afterRollback.getCurrentRuleVersion()).isEqualTo(3);
        assertThat(afterRollback.getHasUnpublishedChanges()).isFalse();
        assertThat(numgenService.nextValue(next)).isEqualTo("SO0003");
        assertThat(longValue("numgen_sequence", "current_value", "gen_key = 'ORDER_NO' AND scope_key = 'GLOBAL'"))
                .isEqualTo(3L);
    }

    @Test
    void sequenceAllocator_usesRealSequenceTable() {
        NumgenSequenceAllocator.Segment first = sequenceAllocator.allocate("ORDER_NO", 2, "GLOBAL", 1L, 3);
        NumgenSequenceAllocator.Segment second = sequenceAllocator.allocate("ORDER_NO", 2, "GLOBAL", 1L, 2);

        assertThat(first.start()).isEqualTo(1L);
        assertThat(first.end()).isEqualTo(3L);
        assertThat(second.start()).isEqualTo(4L);
        assertThat(second.end()).isEqualTo(5L);
        assertThat(longValue("numgen_sequence", "current_value", "gen_key = 'ORDER_NO' AND scope_key = 'GLOBAL'"))
                .isEqualTo(5L);
    }

    @Test
    void sequenceScope_isDerivedFromMarkedSegments() {
        seedScopedRule();
        NumgenNextCommand next = new NumgenNextCommand();
        next.setGenKey("ORDER_NO");
        next.setParams(java.util.Map.of("orgCode", "A1"));

        assertThat(numgenService.nextValue(next)).isEqualTo("SO20260523A10001");
        assertThat(numgenService.nextValue(next)).isEqualTo("SO20260523A10002");

        next.setParams(java.util.Map.of("orgCode", "B2"));
        assertThat(numgenService.nextValue(next)).isEqualTo("SO20260523B20001");

        assertThat(longValue("numgen_sequence", "current_value", "gen_key = 'ORDER_NO' AND scope_key LIKE '%A1%'"))
                .isEqualTo(2L);
        assertThat(longValue("numgen_sequence", "current_value", "gen_key = 'ORDER_NO' AND scope_key LIKE '%B2%'"))
                .isEqualTo(1L);
    }

    private NumgenGeneratorVO firstGenerator() {
        return generatorService.pageGenerators(new NumgenGeneratorPageQuery()).getData().getList().get(0);
    }

    private void seedPublishedAndDraftRules() {
        jdbcTemplate.update("""
                INSERT INTO numgen_generator
                (id, gen_key, gen_name, status, current_rule_version, current_publish_status, tenant_id, del_flag)
                VALUES
                (1, 'ORDER_NO', '订单号规则', 1, 1, 1, 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO numgen_rule
                (id, gen_key, rule_name, version, status, publish_status, version_state, tenant_id, del_flag)
                VALUES
                (101, 'ORDER_NO', '默认规则', 1, 1, 1, 'ACTIVE', 1, 0),
                (102, 'ORDER_NO', '默认规则', 2, 1, 0, 'DRAFT', 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO numgen_rule_segment
                (id, rule_id, sort_order, segment_type, segment_name, literal_value, seq_width, pad_char, sequence_scope, tenant_id)
                VALUES
                (1001, 101, 1, 'TEXT', '固定前缀', 'SO', NULL, '0', 0, 1),
                (1002, 101, 2, 'SEQ', '流水', NULL, 4, '0', 0, 1),
                (2001, 102, 1, 'TEXT', '固定前缀', 'SO', NULL, '0', 0, 1),
                (2002, 102, 2, 'SEQ', '流水', NULL, 4, '0', 0, 1)
                """);
    }

    private void seedScopedRule() {
        jdbcTemplate.update("""
                INSERT INTO numgen_generator
                (id, gen_key, gen_name, status, current_rule_version, current_publish_status, tenant_id, del_flag)
                VALUES
                (1, 'ORDER_NO', '订单号规则', 1, 1, 1, 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO numgen_rule
                (id, gen_key, rule_name, version, status, publish_status, version_state, tenant_id, del_flag)
                VALUES
                (101, 'ORDER_NO', '默认规则', 1, 1, 1, 'ACTIVE', 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO numgen_rule_segment
                (id, rule_id, sort_order, segment_type, segment_name, literal_value, variable_key, date_format, seq_width, pad_char, sequence_scope, tenant_id)
                VALUES
                (1001, 101, 1, 'TEXT', '固定前缀', 'SO', NULL, NULL, NULL, '0', 0, 1),
                (1002, 101, 2, 'TEXT', '测试日期', '20260523', NULL, NULL, NULL, '0', 1, 1),
                (1003, 101, 3, 'PARAM', '组织', NULL, 'orgCode', NULL, NULL, '0', 1, 1),
                (1004, 101, 4, 'SEQ', '流水', NULL, NULL, NULL, 4, '0', 0, 1)
                """);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS numgen_sequence");
        jdbcTemplate.execute("DROP TABLE IF EXISTS numgen_rule_segment");
        jdbcTemplate.execute("DROP TABLE IF EXISTS numgen_rule");
        jdbcTemplate.execute("DROP TABLE IF EXISTS numgen_generator");
        jdbcTemplate.execute("""
                CREATE TABLE numgen_generator (
                    id BIGINT NOT NULL,
                    gen_key VARCHAR(128) NOT NULL,
                    gen_name VARCHAR(128) NOT NULL,
                    status TINYINT NOT NULL DEFAULT 1,
                    current_rule_version INT,
                    current_publish_status TINYINT NOT NULL DEFAULT 0,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    del_flag TINYINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_numgen_generator_tenant_key (tenant_id, gen_key, del_flag)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE numgen_rule (
                    id BIGINT NOT NULL,
                    gen_key VARCHAR(128) NOT NULL,
                    rule_name VARCHAR(128) NOT NULL,
                    version INT NOT NULL DEFAULT 1,
                    status TINYINT NOT NULL DEFAULT 1,
                    publish_status TINYINT NOT NULL DEFAULT 0,
                    version_state VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    del_flag TINYINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_numgen_rule_tenant_key_version (tenant_id, gen_key, version, del_flag)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE numgen_rule_segment (
                    id BIGINT NOT NULL,
                    rule_id BIGINT NOT NULL,
                    sort_order INT NOT NULL,
                    segment_type VARCHAR(32) NOT NULL,
                    segment_name VARCHAR(128) NOT NULL,
                    literal_value VARCHAR(128),
                    variable_key VARCHAR(128),
                    date_format VARCHAR(64),
                    seq_width INT,
                    pad_char VARCHAR(1) DEFAULT '0',
                    sequence_scope TINYINT NOT NULL DEFAULT 0,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE numgen_sequence (
                    id BIGINT NOT NULL,
                    gen_key VARCHAR(128) NOT NULL,
                    rule_version INT NOT NULL DEFAULT 1,
                    scope_key VARCHAR(256) NOT NULL DEFAULT 'GLOBAL',
                    current_value BIGINT NOT NULL DEFAULT 0,
                    version INT NOT NULL DEFAULT 0,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    create_by VARCHAR(64),
                    update_by VARCHAR(64),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_numgen_sequence_tenant_scope (tenant_id, gen_key, scope_key)
                )
                """);
    }

    private Integer intValue(String table, String column, String where) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE " + where, Integer.class);
    }

    private Long longValue(String table, String column, String where) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE " + where, Long.class);
    }

    private String stringValue(String table, String column, String where) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE " + where, String.class);
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackages = "io.mango.numgen.core.mapper")
    @ComponentScan(basePackages = "io.mango.numgen.core.service.impl")
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
