package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowApplyAttributeCondition;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务分页按申请可筛属性过滤的集成测试。
 *
 * <p>用真实列名夹具验证属性交集、缺属性不匹配、多值字段与申请时间过滤；属性过滤必须和计数、
 * 分页在同一段 SQL 条件内完成，不能退化成内存过滤。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskAttributeFilterIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_attribute_filter;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/workflow/*.xml"
})
class WorkflowTaskAttributeFilterIntegrationTest {

    private static final String BUSINESS_TYPE = "BAOHAN_RC_PROJECT_REVIEW";
    private static final Long TENANT_ID = 1L;
    private static final AtomicLong ATTRIBUTE_ID = new AtomicLong(1000L);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowTaskHistoryMapper taskHistoryMapper;

    @BeforeEach
    void rebuild() {
        rebuildTables();
        insertData();
    }

    @Test
    void filtersByOneAttributeWithSingleValue() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(condition("guaranteeTypeCode", "BID")));

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3");
        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
    }

    @Test
    void filtersByOneAttributeWithMultipleValues() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(
                condition("guaranteeTypeCode", "PERF", "BID")));

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3", "T2", "T1");
    }

    @Test
    void filtersByMultipleAttributesWithAndSemantics() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(
                condition("guaranteeTypeCode", "PERF"),
                condition("beneficiary", "中铁三局")));

        assertThat(taskIdsOf(criteria)).containsExactly("T2", "T1");
        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
    }

    @Test
    void excludesAppliesWithoutTheFilteredAttribute() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(condition("auditType", "RISK")));

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3");
    }

    @Test
    void matchesAnyValueOfMultiValuedAttribute() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(condition("auditType", "LEGAL")));

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3");
    }

    @Test
    void combinesAttributeFilterWithApplyTimeRange() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyAttributeConditions(List.of(condition("guaranteeTypeCode", "PERF", "BID")))
                .applyTimeBegin(LocalDateTime.of(2026, 1, 15, 0, 0))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3");
    }

    @Test
    void keepsTotalAndPagesConsistentWithAttributeFilter() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of(
                condition("guaranteeTypeCode", "PERF", "BID")));

        long total = taskHistoryMapper.countTasks(criteria);
        List<String> firstPage = taskIdsOf(criteria, 2, 0L);
        List<String> secondPage = taskIdsOf(criteria, 2, 2L);

        assertThat(total).isEqualTo(4L);
        assertThat(firstPage).containsExactly("T4", "T3");
        assertThat(secondPage).containsExactly("T2", "T1");
    }

    @Test
    void sortsByApplyTimeWhenRequested() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("applyTime")
                .sortDirection("asc")
                .build();

        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);

        assertThat(rows).extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1", "T2", "T3", "T4");
    }

    @Test
    void returnsAllTasksWhenNoAttributeFilterIsGiven() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of());

        assertThat(taskIdsOf(criteria)).containsExactly("T4", "T3", "T2", "T1");
    }

    @Test
    void isolatesAttributesByTenant() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(2L)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyAttributeConditions(List.of(condition("guaranteeTypeCode", "PERF")))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskIdsOf(criteria)).containsExactly("T5");
    }

    private WorkflowApplyAttributeCondition condition(String key, String... values) {
        return WorkflowApplyAttributeCondition.builder().key(key).values(List.of(values)).build();
    }

    private WorkflowTaskHistoryCriteria criteria(List<WorkflowApplyAttributeCondition> conditions) {
        return WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyAttributeConditions(conditions)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
    }

    private List<String> taskIdsOf(WorkflowTaskHistoryCriteria criteria) {
        return taskIdsOf(criteria, 10, 0L);
    }

    private List<String> taskIdsOf(WorkflowTaskHistoryCriteria criteria, int limit, long offset) {
        return taskHistoryMapper.selectTasks(criteria, limit, offset).stream()
                .map(WorkflowTaskHistoryRow::getTaskId)
                .toList();
    }

    private void insertData() {
        insertApply(10L, TENANT_ID, "RC1", "P1", LocalDateTime.of(2026, 1, 1, 9, 0));
        insertApply(30L, TENANT_ID, "RC3", "P3", LocalDateTime.of(2026, 2, 1, 9, 0));
        insertApply(50L, 2L, "RC5", "P5", LocalDateTime.of(2026, 1, 1, 9, 0));

        insertAttribute(10L, TENANT_ID, "guaranteeTypeCode", "PERF");
        insertAttribute(10L, TENANT_ID, "beneficiary", "中铁三局");
        insertAttribute(30L, TENANT_ID, "guaranteeTypeCode", "BID");
        insertAttribute(30L, TENANT_ID, "beneficiary", "中铁四局");
        insertAttribute(30L, TENANT_ID, "auditType", "RISK");
        insertAttribute(30L, TENANT_ID, "auditType", "LEGAL");
        insertAttribute(50L, 2L, "guaranteeTypeCode", "PERF");

        insertTask("T1", "P1", LocalDateTime.of(2026, 1, 1, 10, 0));
        insertTask("T2", "P1", LocalDateTime.of(2026, 1, 2, 10, 0));
        insertTask("T3", "P3", LocalDateTime.of(2026, 2, 3, 10, 0));
        insertTask("T4", "P3", LocalDateTime.of(2026, 2, 4, 10, 0));
        insertTask("T5", "P5", LocalDateTime.of(2026, 1, 5, 10, 0));
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String processInstanceId,
                             LocalDateTime createdTime) {
        jdbcTemplate.update("insert into workflow_business_apply "
                        + "(id, tenant_id, org_id, business_type, business_key, apply_status, "
                        + "process_instance_id, process_name, process_definition_key, apply_code, "
                        + "apply_title, apply_summary, created_time) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, 200L, BUSINESS_TYPE, businessKey, "IN_APPROVAL", processInstanceId,
                "项目审批", "rc_project_review", "GR" + id, "保函风控审核", "履约保函", createdTime);
    }

    private void insertAttribute(Long applyId, Long tenantId, String key, String value) {
        jdbcTemplate.update("insert into workflow_business_apply_attribute "
                        + "(id, tenant_id, apply_id, business_type, attr_key, attr_value) "
                        + "values (?, ?, ?, ?, ?, ?)",
                ATTRIBUTE_ID.incrementAndGet(), tenantId, applyId, BUSINESS_TYPE, key, value);
    }

    private void insertTask(String taskId, String processInstanceId, LocalDateTime createTime) {
        jdbcTemplate.update("insert into act_hi_taskinst "
                        + "(ID_, NAME_, TASK_DEF_KEY_, PROC_INST_ID_, PROC_DEF_ID_, ASSIGNEE_, START_TIME_) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                taskId, "初审", "FIRST_REVIEW", processInstanceId, "PD1", "alice", createTime);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists act_hi_taskinst");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_attribute");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("""
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_definition_key varchar(128),
                    action varchar(64),
                    variables_json text,
                    created_time timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table act_hi_taskinst (
                    ID_ varchar(64) not null,
                    NAME_ varchar(255),
                    TASK_DEF_KEY_ varchar(128),
                    PROC_INST_ID_ varchar(64),
                    PROC_DEF_ID_ varchar(128),
                    ASSIGNEE_ varchar(128),
                    START_TIME_ timestamp,
                    CLAIM_TIME_ timestamp,
                    END_TIME_ timestamp,
                    DELETE_REASON_ varchar(255),
                    primary key (ID_)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    business_type varchar(128),
                    business_key varchar(128),
                    apply_status varchar(64),
                    applicant_dept_id bigint,
                    process_instance_id varchar(128),
                    process_name varchar(255),
                    process_definition_key varchar(128),
                    apply_code varchar(128),
                    apply_title varchar(255),
                    apply_summary varchar(1000),
                    created_time timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_attribute (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    apply_id bigint,
                    business_type varchar(128),
                    attr_key varchar(64),
                    attr_value varchar(255),
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
    }

    @Configuration
    @Import(MybatisPlusAutoConfiguration.class)
    @MapperScan("io.mango.workflow.core.mapper")
    static class TestConfig {
    }
}
