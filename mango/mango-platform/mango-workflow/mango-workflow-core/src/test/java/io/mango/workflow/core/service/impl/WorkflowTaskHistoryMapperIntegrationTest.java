package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import io.mango.workflow.core.model.WorkflowTaskIdentityLinkRow;
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
 * 任务历史只读查询集成测试。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskHistoryMapperIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_history;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/workflow/*.xml"
})
class WorkflowTaskHistoryMapperIntegrationTest {

    private static final String BUSINESS_TYPE = "BAOHAN_RC_PROJECT_REVIEW";
    private static final Long TENANT_ID = 1L;

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
    void returnsAllTasksOfOneBusinessKeyInCreateTimeDescOrder() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);
        assertThat(rows).extracting(WorkflowTaskHistoryRow::getTaskId).containsExactly("T2", "T1");
        assertThat(rows.get(1).getClaimTime()).isNotNull();
        assertThat(rows.get(1).getEndTime()).isNotNull();
        assertThat(rows.get(0).getOrgId()).isEqualTo(200L);
        assertThat(rows.get(0).getApplyStatus()).isEqualTo("IN_APPROVAL");
    }

    @Test
    void filtersByBusinessKeysBatch() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1", "RC2"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(3L);
        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactlyInAnyOrder("T1", "T2", "T3");
    }

    @Test
    void isolatesTenant() {
        WorkflowTaskHistoryCriteria otherTenant = WorkflowTaskHistoryCriteria.builder()
                .tenantId(2L)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC3"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.countTasks(otherTenant)).isEqualTo(1L);
        assertThat(taskHistoryMapper.selectTasks(criteria(List.of("RC3")), 10, 0L)).isEmpty();
    }

    @Test
    void filtersSelfScopeByAssigneeOrCandidate() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .selfScope(true)
                .selfIdentifiers(List.of("alice"))
                .selfCandidateGroups(List.of("ROLE_REVIEWER"))
                .sortColumn("createTime")
                .sortDirection("asc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1", "T2");
    }

    @Test
    void filtersSelfScopeByCandidateGroup() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .selfScope(true)
                .selfIdentifiers(List.of("nobody"))
                .selfCandidateGroups(List.of("ROLE_REVIEWER"))
                .sortColumn("createTime")
                .sortDirection("asc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
    }

    @Test
    void filtersTaskStatuses() {
        WorkflowTaskHistoryCriteria todoOnly = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(false)
                .includeCanceled(false)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria canceledOnly = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(false)
                .includeDone(false)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(todoOnly, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
        assertThat(taskHistoryMapper.selectTasks(canceledOnly, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3");
    }

    @Test
    void filtersByClaimStatusAndAssignee() {
        WorkflowTaskHistoryCriteria unclaimed = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .assigneeRequired(false)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria byAssignee = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .assigneeIds(List.of(1001L))
                .assigneeIdentifiers(List.of("1001"))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(unclaimed, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
        assertThat(taskHistoryMapper.selectTasks(byAssignee, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1");
    }

    @Test
    void sortsEndTimeWithNullsLastInBothDirections() {
        WorkflowTaskHistoryCriteria asc = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("endTime")
                .sortDirection("asc")
                .build();
        WorkflowTaskHistoryCriteria desc = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("endTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(asc, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1", "T3", "T2");
        assertThat(taskHistoryMapper.selectTasks(desc, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3", "T1", "T2");
    }

    @Test
    void filtersByApplyStatusAndReturnedFlag() {
        WorkflowTaskHistoryCriteria rejected = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyStatuses(List.of("REJECTED"))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria returned = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .returned(Boolean.TRUE)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(rejected, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3");
        assertThat(taskHistoryMapper.selectTasks(returned, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactlyInAnyOrder("T1", "T2", "T3");
    }

    @Test
    void exposesTaskLevelReturnFactAndLatestReturnTarget() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC2"));

        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);

        assertThat(rows).hasSize(1);
        WorkflowTaskHistoryRow canceled = rows.get(0);
        assertThat(canceled.getTaskId()).isEqualTo("T3");
        assertThat(canceled.getDeleteReason()).isEqualTo("审批驳回");
        assertThat(canceled.getReturned()).isTrue();
        assertThat(canceled.getTaskReturned()).isTrue();
        assertThat(canceled.getLastReturnAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 12, 0));
        assertThat(canceled.getLastReturnVariables()).contains("risk_specialist_review");
    }

    @Test
    void returnsCandidateLinksForRequestedTasks() {
        List<WorkflowTaskIdentityLinkRow> links = taskHistoryMapper.selectCandidateLinks(List.of("T2"));

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getUserId()).isEqualTo("alice");
        assertThat(links.get(0).getGroupId()).isEqualTo("ROLE_REVIEWER");
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
        insertApply(10L, TENANT_ID, "RC1", "IN_APPROVAL", 200L, "P1");
        insertApply(20L, TENANT_ID, "RC2", "REJECTED", 300L, "P2");
        insertApply(30L, 2L, "RC3", "IN_APPROVAL", 400L, "P3");
        insertTask("T1", "初审", "FIRST_REVIEW", "P1", "alice",
                LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 10, 0, 30),
                LocalDateTime.of(2026, 1, 1, 11, 0), null);
        insertTask("T2", "复审", "RECHECK", "P1", null,
                LocalDateTime.of(2026, 1, 2, 10, 0), null, null, null);
        insertTask("T3", "初审", "FIRST_REVIEW", "P2", "bob",
                LocalDateTime.of(2026, 1, 3, 10, 0), null,
                LocalDateTime.of(2026, 1, 3, 11, 0), "审批驳回");
        insertTask("T4", "初审", "FIRST_REVIEW", "P3", "carol",
                LocalDateTime.of(2026, 1, 4, 10, 0), null, null, null);
        jdbcTemplate.update("insert into act_hi_identitylink (ID_, TASK_ID_, TYPE_, USER_ID_, GROUP_ID_) "
                + "values (?, ?, ?, ?, ?)", "IL1", "T2", "candidate", "alice", "ROLE_REVIEWER");
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                100L, TENANT_ID, "P1", "T1", "FIRST_REVIEW", "RETURN",
                "{\"targetTaskDefinitionKey\":\"FIRST_REVIEW\"}", LocalDateTime.of(2026, 1, 2, 9, 0));
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                101L, TENANT_ID, "P2", "T3", "FIRST_REVIEW", "RETURN",
                "{\"targetTaskDefinitionKey\":\"risk_specialist_review\"}", LocalDateTime.of(2026, 1, 3, 12, 0));
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1001L, "alice");
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String status,
                             Long deptId, String processInstanceId) {
        jdbcTemplate.update("insert into workflow_business_apply "
                        + "(id, tenant_id, org_id, business_type, business_key, apply_status, "
                        + "applicant_dept_id, process_instance_id, process_name, process_definition_key) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, deptId, BUSINESS_TYPE, businessKey, status, deptId, processInstanceId,
                "项目审批", "rc_project_review");
    }

    private void insertTask(String taskId, String name, String taskDefinitionKey, String processInstanceId,
                            String assignee, LocalDateTime createTime, LocalDateTime claimTime,
                            LocalDateTime endTime, String deleteReason) {
        jdbcTemplate.update("insert into act_hi_taskinst "
                        + "(ID_, NAME_, TASK_DEF_KEY_, PROC_INST_ID_, PROC_DEF_ID_, ASSIGNEE_, "
                        + "START_TIME_, CLAIM_TIME_, END_TIME_, DELETE_REASON_) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                taskId, name, taskDefinitionKey, processInstanceId, "PD1", assignee,
                createTime, claimTime, endTime, deleteReason);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists act_hi_taskinst");
        jdbcTemplate.execute("drop table if exists act_hi_identitylink");
        jdbcTemplate.execute("drop table if exists act_ru_identitylink");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists identity_user");
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
    }

    @Configuration
    @Import(MybatisPlusAutoConfiguration.class)
    @MapperScan("io.mango.workflow.core.mapper")
    static class TestConfig {
    }
}
