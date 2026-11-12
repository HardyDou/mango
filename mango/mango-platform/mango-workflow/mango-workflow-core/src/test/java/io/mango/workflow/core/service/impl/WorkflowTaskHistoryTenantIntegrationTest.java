package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import io.mango.workflow.core.model.WorkflowTaskIdentityLinkRow;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 租户插件启用时的任务历史查询集成测试。
 *
 * <p>Flowable 引擎表只有 {@code TENANT_ID_}，没有 Mango 租户列 {@code tenant_id}；租户插件必须按
 * {@code act_*} 前缀跳过这些表，否则自定义查询会以“未知列 tenant_id”失败（Mango Issue #1220）。
 * 本测试保持平台默认租户配置（不关闭、不覆盖排除表），覆盖计数、分页、候选身份关联和租户收敛。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskHistoryTenantIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_history_tenant;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/workflow/*.xml"
})
class WorkflowTaskHistoryTenantIntegrationTest {

    private static final String BUSINESS_TYPE = "BAOHAN_RC_PROJECT_REVIEW";
    private static final Long TENANT_ID = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowTaskHistoryMapper taskHistoryMapper;

    @BeforeEach
    void rebuild() {
        MangoContextHolder.set(MangoContextSnapshot.request(null, null, String.valueOf(TENANT_ID), null, null));
        rebuildTables();
        insertData();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void countsAndSelectsEngineHistoryTasksWithTenantInterceptorEnabled() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(1L);
        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1");
    }

    @Test
    void keepsBusinessApplyTenantIsolationUnderTenantInterceptor() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1", "RC2"));

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1");
    }

    @Test
    void selectsCandidateLinksFromBothHistoryAndRuntimeEngineTables() {
        List<WorkflowTaskIdentityLinkRow> links = taskHistoryMapper.selectCandidateLinks(List.of("T1"));

        assertThat(links).hasSize(2);
        assertThat(links).extracting(WorkflowTaskIdentityLinkRow::getUserId)
                .containsExactlyInAnyOrder("alice", "bob");
        assertThat(links).extracting(WorkflowTaskIdentityLinkRow::getGroupId)
                .containsOnly("ROLE_REVIEWER");
    }

    @Test
    void filtersSelfScopeByEngineCandidateLinks() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .selfScope(true)
                .selfIdentifiers(List.of("bob"))
                .selfCandidateGroups(List.of("ROLE_REVIEWER"))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1");
    }

    private WorkflowTaskHistoryCriteria criteria(List<String> businessKeys) {
        return WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(businessKeys)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
    }

    private void insertData() {
        insertApply(10L, TENANT_ID, "RC1", "P1");
        insertApply(20L, 2L, "RC2", "P2");
        insertTask("T1", "初审", "FIRST_REVIEW", "P1", "alice",
                LocalDateTime.of(2026, 1, 1, 10, 0), null);
        insertTask("T2", "初审", "FIRST_REVIEW", "P2", "carol",
                LocalDateTime.of(2026, 1, 2, 10, 0), LocalDateTime.of(2026, 1, 2, 11, 0));
        jdbcTemplate.update("insert into act_hi_identitylink (ID_, TASK_ID_, TYPE_, USER_ID_, GROUP_ID_) "
                + "values (?, ?, ?, ?, ?)", "IL1", "T1", "candidate", "alice", "ROLE_REVIEWER");
        jdbcTemplate.update("insert into act_ru_identitylink (ID_, TASK_ID_, TYPE_, USER_ID_, GROUP_ID_) "
                + "values (?, ?, ?, ?, ?)", "IL2", "T1", "candidate", "bob", "ROLE_REVIEWER");
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1001L, "alice");
        jdbcTemplate.update("insert into tenant_member (id, tenant_id, user_id) values (?, ?, ?)",
                5001L, TENANT_ID, 1001L);
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String processInstanceId) {
        jdbcTemplate.update("insert into workflow_business_apply "
                        + "(id, tenant_id, org_id, business_type, business_key, apply_status, "
                        + "applicant_dept_id, process_instance_id, process_name, process_definition_key) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, 200L, BUSINESS_TYPE, businessKey, "IN_APPROVAL", 200L, processInstanceId,
                "项目审批", "rc_project_review");
    }

    private void insertTask(String taskId, String name, String taskDefinitionKey, String processInstanceId,
                            String assignee, LocalDateTime createTime, LocalDateTime endTime) {
        jdbcTemplate.update("insert into act_hi_taskinst "
                        + "(ID_, NAME_, TASK_DEF_KEY_, PROC_INST_ID_, PROC_DEF_ID_, ASSIGNEE_, "
                        + "START_TIME_, END_TIME_, DELETE_REASON_) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                taskId, name, taskDefinitionKey, processInstanceId, "PD1", assignee,
                createTime, endTime, null);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists act_hi_taskinst");
        jdbcTemplate.execute("drop table if exists act_hi_identitylink");
        jdbcTemplate.execute("drop table if exists act_ru_identitylink");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("drop table if exists tenant_member");
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
                create table act_hi_identitylink (
                    ID_ varchar(64) not null,
                    TASK_ID_ varchar(64),
                    TYPE_ varchar(64),
                    USER_ID_ varchar(128),
                    GROUP_ID_ varchar(128),
                    primary key (ID_)
                )
                """);
        jdbcTemplate.execute("""
                create table act_ru_identitylink (
                    ID_ varchar(64) not null,
                    TASK_ID_ varchar(64),
                    TYPE_ varchar(64),
                    USER_ID_ varchar(128),
                    GROUP_ID_ varchar(128),
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
                    primary key (id)
                )
                """);
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
                create table identity_user (
                    id bigint not null,
                    username varchar(128),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint not null,
                    tenant_id bigint,
                    user_id bigint,
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
