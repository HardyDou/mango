package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import org.flowable.engine.TaskService;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowBusinessApplyServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_business_apply_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class WorkflowBusinessApplyServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowBusinessApplyService service;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        rebuildTables();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void mySummaryCountsCurrentUserBusinessApplyStatusesThroughRealMapper() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "default", "USER", "USER", 1L, "internal-admin"));
        insertApply(1L, 1001L, "SUBMITTED");
        insertApply(2L, 1001L, "IN_APPROVAL");
        insertApply(3L, 1001L, "IN_APPROVAL");
        insertApply(4L, 1001L, "APPROVED");
        insertApply(5L, 1001L, "REJECTED");
        insertApply(6L, 1001L, "WITHDRAWN");
        insertApply(7L, 1002L, "IN_APPROVAL");
        insertApply(8L, 1001L, "DRAFT");

        var summary = service.mySummary();

        assertThat(summary.getInReview()).isEqualTo(3L);
        assertThat(summary.getCompleted()).isEqualTo(1L);
        assertThat(summary.getRejected()).isEqualTo(1L);
        assertThat(summary.getWithdrawn()).isEqualTo(1L);
    }

    @Test
    void mySummaryReturnsZeroWhenUserContextMissing() {
        insertApply(1L, 1001L, "IN_APPROVAL");

        var summary = service.mySummary();

        assertThat(summary.getInReview()).isZero();
        assertThat(summary.getCompleted()).isZero();
        assertThat(summary.getRejected()).isZero();
        assertThat(summary.getWithdrawn()).isZero();
    }

    @Test
    void detailDerivesCustomViewPathFromFormSnapshot() {
        insertApply(10L, 1001L, "APPROVED");
        jdbcTemplate.update("""
                update workflow_business_apply
                set form_json_snapshot = ?
                where id = ?
                """, "{\"mode\":\"CUSTOM\",\"customConfig\":{\"viewPath\":\" /expense/apply/detail \"}}", 10L);

        var detail = service.detail(10L);

        assertThat(detail.getViewPath()).isEqualTo("/expense/apply/detail");
    }

    @Test
    void markWithdrawnPersistsTerminalStateClearsTasksAndWritesAuditLogThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "applicant", "default", "USER", "USER", 1L, "internal-admin"));
        insertApply(20L, 1001L, "IN_APPROVAL");
        jdbcTemplate.update("""
                update workflow_business_apply
                set process_instance_id = 'PROC-20',
                    current_task_names = '财务审批',
                    current_task_definition_keys = 'finance_approve',
                    current_assignee_names = 'approver'
                where id = 20
                """);
        insertCurrentTask(200L, 20L, "PROC-20");

        var locked = service.lockWithdrawalTarget(20L, null);
        var withdrawn = service.markWithdrawn("PROC-20", " 申请资料有误 ");

        assertThat(locked.getApplyStatus().name()).isEqualTo("IN_APPROVAL");
        assertThat(withdrawn.getApplyStatus().name()).isEqualTo("WITHDRAWN");
        assertThat(withdrawn.getCurrentTasks()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                select apply_status from workflow_business_apply where id = 20
                """, String.class)).isEqualTo("WITHDRAWN");
        assertThat(jdbcTemplate.queryForMap("""
                select current_task_names, current_task_definition_keys, current_assignee_names
                from workflow_business_apply where id = 20
                """)).allSatisfy((key, value) -> assertThat(value).isNull());
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from workflow_business_apply_current_task where apply_id = 20
                """, Long.class)).isZero();
        assertThat(jdbcTemplate.queryForMap("""
                select tenant_id, apply_id, from_status, to_status, action, action_name,
                       operator_id, operator_name, comment, process_instance_id
                from workflow_business_apply_status_log where apply_id = 20
                """))
                .containsEntry("tenant_id", 1L)
                .containsEntry("apply_id", 20L)
                .containsEntry("from_status", "IN_APPROVAL")
                .containsEntry("to_status", "WITHDRAWN")
                .containsEntry("action", "WITHDRAW")
                .containsEntry("action_name", "撤回")
                .containsEntry("operator_id", 1001L)
                .containsEntry("operator_name", "applicant")
                .containsEntry("comment", "申请资料有误")
                .containsEntry("process_instance_id", "PROC-20");
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists workflow_business_apply_current_task");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_status_log");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    apply_code varchar(128),
                    business_type varchar(128),
                    business_key varchar(128),
                    apply_title varchar(255),
                    apply_summary varchar(1000),
                    applicant_id bigint,
                    applicant_name varchar(128),
                    applicant_dept_id bigint,
                    applicant_dept_name varchar(128),
                    process_definition_id bigint,
                    process_definition_key varchar(128),
                    engine_process_definition_id varchar(128),
                    process_instance_id varchar(128),
                    process_name varchar(255),
                    apply_status varchar(64),
                    current_task_names varchar(1000),
                    current_task_definition_keys varchar(1000),
                    current_assignee_names varchar(1000),
                    render_mode varchar(64),
                    apply_page_key varchar(128),
                    approve_page_key varchar(128),
                    form_key varchar(128),
                    form_version int,
                    form_json_snapshot text,
                    form_data_snapshot text,
                    snapshot_ref varchar(255),
                    snapshot_digest varchar(128),
                    variables_json text,
                    extension_json text,
                    reapply_from_apply_id bigint,
                    latest_flag boolean,
                    created_by bigint,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_time timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_current_task (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    apply_id bigint,
                    business_type varchar(128),
                    business_key varchar(128),
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_definition_key varchar(128),
                    task_name varchar(255),
                    assignee_id bigint,
                    assignee_name varchar(128),
                    claim_status varchar(64),
                    candidate_users text,
                    candidate_groups text,
                    arrived_at timestamp,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_status_log (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    apply_id bigint,
                    from_status varchar(64),
                    to_status varchar(64),
                    action varchar(64),
                    action_name varchar(128),
                    operator_id bigint,
                    operator_name varchar(128),
                    comment varchar(1000),
                    task_id varchar(128),
                    task_definition_key varchar(128),
                    process_instance_id varchar(128),
                    created_by bigint,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_time timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
    }

    private void insertApply(Long id, Long applicantId, String status) {
        jdbcTemplate.update("""
                insert into workflow_business_apply (
                    id, tenant_id, apply_code, business_type, business_key, apply_title,
                    applicant_id, applicant_name, apply_status, latest_flag, created_at, updated_at
                ) values (?, 1, ?, 'RESOURCE', ?, '资源申请', ?, '测试申请人', ?, true, ?, ?)
                """, id, "APPLY-" + id, "BIZ-" + id, applicantId, status, LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertCurrentTask(Long id, Long applyId, String processInstanceId) {
        jdbcTemplate.update("""
                insert into workflow_business_apply_current_task (
                    id, tenant_id, apply_id, business_type, business_key, process_instance_id,
                    task_id, task_definition_key, task_name, assignee_id, assignee_name,
                    claim_status, arrived_at, created_at, updated_at
                ) values (?, 1, ?, 'RESOURCE', ?, ?, ?, 'finance_approve', '财务审批',
                          2001, 'approver', 'ASSIGNED', ?, ?, ?)
                """, id, applyId, "BIZ-" + applyId, processInstanceId, "TASK-" + id,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Configuration
    @Import(WorkflowBusinessApplyService.class)
    @MapperScan("io.mango.workflow.core.mapper")
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TaskService taskService() {
            return mock(TaskService.class);
        }
    }
}
