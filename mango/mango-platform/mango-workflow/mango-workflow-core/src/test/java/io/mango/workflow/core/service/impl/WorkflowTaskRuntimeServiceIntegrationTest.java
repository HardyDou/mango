package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.api.WorkflowEventTypes;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyCurrentTaskMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.mapper.WorkflowCopiedTaskMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = WorkflowTaskRuntimeServiceIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:workflow_task_runtime_it;MODE=LEGACY;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=false",
                "flowable.database-schema-update=true",
                "flowable.async-executor-activate=false",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.mybatis-plus.tenant.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        })
class WorkflowTaskRuntimeServiceIntegrationTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private IWorkflowBusinessApplyService businessApplyService;
    @Autowired
    private IWorkflowTaskRuntimeService taskRuntimeService;
    @Autowired
    private CollectingDomainEventPublisher eventPublisher;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "default", "USER", "ORG", 1001L, "internal-admin"));
        jdbcTemplate = new JdbcTemplate(dataSource);
        eventPublisher.clear();
        rebuildWorkflowTables();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void completeWithResultPublishesAdvancedEventAfterCurrentTasksRefresh() {
        repositoryService.createDeployment()
                .name("issue-233-runtime")
                .addString("issue-233.bpmn20.xml", twoStepApprovalBpmn())
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("issue_233_runtime")
                .latestVersion()
                .singleResult();

        CreateWorkflowBusinessApplyCommand applyCommand = new CreateWorkflowBusinessApplyCommand();
        applyCommand.setBusinessType("ISSUE_233");
        applyCommand.setBusinessKey("BIZ-233");
        applyCommand.setApplyTitle("Issue 233 集成测试");
        applyCommand.setProcessDefinitionKey("issue_233_runtime");
        Long applyId = businessApplyService.create(applyCommand).getData().getId();

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("issue_233_runtime", "BIZ-233", Map.of(
                "businessType", "ISSUE_233",
                "businessKey", "BIZ-233",
                "applyId", String.valueOf(applyId),
                "mangoInitiator", "admin"
        ));
        businessApplyService.markProcessStarted(
                applyId,
                233L,
                "issue_233_runtime",
                processDefinition.getId(),
                processDefinition.getName(),
                instance.getProcessInstanceId());

        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        assertThat(managerTask).isNotNull();

        CompleteWorkflowTaskCommand command = new CompleteWorkflowTaskCommand();
        command.setTaskId(managerTask.getId());
        command.setComment("同意");
        var result = taskRuntimeService.completeWithResult(command).getData();

        assertThat(result.getEnded()).isFalse();
        assertThat(result.getApplyId()).isEqualTo(applyId);
        assertThat(result.getApplyStatus()).isEqualTo(WorkflowApplyStatus.IN_APPROVAL);
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("finance_approve", task -> task.getTaskDefinitionKey())
                .returns("finance", task -> task.getAssigneeName());

        assertThat(currentTaskDefinitionKeys(applyId)).containsExactly("finance_approve");

        List<String> eventTypes = eventPublisher.events().stream()
                .map(DomainEvent::getEventType)
                .toList();
        assertThat(eventTypes).contains(WorkflowEventTypes.TASK_COMPLETED, WorkflowEventTypes.TASK_ADVANCED);
        assertThat(eventTypes.indexOf(WorkflowEventTypes.TASK_COMPLETED))
                .isLessThan(eventTypes.indexOf(WorkflowEventTypes.TASK_ADVANCED));

        DomainEvent advancedEvent = eventPublisher.events().stream()
                .filter(event -> WorkflowEventTypes.TASK_ADVANCED.equals(event.getEventType()))
                .findFirst()
                .orElseThrow();
        assertThat(advancedEvent.getPayload())
                .containsEntry("completedTaskDefinitionKey", "manager_approve")
                .containsEntry("ended", false)
                .containsEntry("applyId", applyId)
                .containsEntry("currentTaskDefinitionKeys", "finance_approve")
                .containsEntry("currentAssigneeNames", "finance");
        assertThat((List<?>) advancedEvent.getPayload().get("currentTasks")).singleElement()
                .satisfies(currentTask -> assertThat(((Map<?, ?>) currentTask).get("taskDefinitionKey"))
                        .isEqualTo("finance_approve"));
    }

    private List<String> currentTaskDefinitionKeys(Long applyId) {
        return jdbcTemplate.queryForList("""
                select task_definition_key
                from workflow_business_apply_current_task
                where apply_id = ?
                order by arrived_at
                """, String.class, applyId);
    }

    private void rebuildWorkflowTables() {
        jdbcTemplate.execute("drop table if exists workflow_business_apply_current_task");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_status_log");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_copied_task");
        jdbcTemplate.execute("drop table if exists workflow_form_instance");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    tenant_id bigint,
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
                    apply_id bigint,
                    business_type varchar(128),
                    business_key varchar(128),
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_definition_key varchar(255),
                    task_name varchar(255),
                    assignee_id bigint,
                    assignee_name varchar(128),
                    arrived_at timestamp,
                    created_at timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_status_log (
                    id bigint not null,
                    tenant_id bigint,
                    apply_id bigint,
                    from_status varchar(64),
                    to_status varchar(64),
                    action varchar(64),
                    action_name varchar(128),
                    operator_id bigint,
                    operator_name varchar(128),
                    comment varchar(1000),
                    task_id varchar(128),
                    task_definition_key varchar(255),
                    process_instance_id varchar(128),
                    created_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_name varchar(255),
                    task_definition_key varchar(255),
                    action varchar(64),
                    action_name varchar(128),
                    operator_id bigint,
                    operator_name varchar(128),
                    comment varchar(1000),
                    variables_json text,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_form_instance (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    business_key varchar(128),
                    definition_id bigint,
                    definition_key varchar(128),
                    definition_name varchar(255),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    form_code varchar(128),
                    form_json text,
                    variables_json text,
                    status varchar(64),
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
                create table workflow_copied_task (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    process_definition_id varchar(128),
                    process_name varchar(255),
                    process_key varchar(128),
                    business_key varchar(128),
                    node_definition_key varchar(255),
                    node_name varchar(255),
                    copied_user_id varchar(128),
                    copied_user_name varchar(128),
                    message varchar(1000),
                    read_flag boolean,
                    read_time timestamp,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
    }

    private String twoStepApprovalBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xmlns:flowable="http://flowable.org/bpmn"
                             xmlns:mango="http://mango.io/workflow"
                             targetNamespace="http://mango.io/workflow/test">
                  <process id="issue_233_runtime" name="Issue 233 Runtime" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="flow_start_manager" sourceRef="start" targetRef="manager_approve"/>
                    <userTask id="manager_approve" name="经理审批" flowable:assignee="admin">
                      <extensionElements>
                        <mango:mangoApprovalConfig><![CDATA[{"actions":{"complete":{"enabled":true}}}]]></mango:mangoApprovalConfig>
                      </extensionElements>
                    </userTask>
                    <sequenceFlow id="flow_manager_finance" sourceRef="manager_approve" targetRef="finance_approve"/>
                    <userTask id="finance_approve" name="财务审批" flowable:assignee="finance">
                      <extensionElements>
                        <mango:mangoApprovalConfig><![CDATA[{"actions":{"complete":{"enabled":true}}}]]></mango:mangoApprovalConfig>
                      </extensionElements>
                    </userTask>
                    <sequenceFlow id="flow_finance_end" sourceRef="finance_approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @Import({
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            WorkflowAssigneeResolver.class,
            WorkflowCandidateGroupProvider.class,
            WorkflowEventPublisher.class,
            WorkflowBusinessApplyServiceImpl.class,
            WorkflowTaskRuntimeServiceImpl.class
    })
    @MapperScan(basePackageClasses = {
            WorkflowBusinessApplyMapper.class,
            WorkflowBusinessApplyCurrentTaskMapper.class,
            WorkflowBusinessApplyStatusLogMapper.class,
            WorkflowCopiedTaskMapper.class,
            WorkflowDefinitionMapper.class,
            WorkflowFormInstanceMapper.class,
            WorkflowTaskRecordMapper.class
    })
    static class TestApplication {

        @Bean
        CollectingDomainEventPublisher collectingDomainEventPublisher() {
            return new CollectingDomainEventPublisher();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    static class CollectingDomainEventPublisher implements IDomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        List<DomainEvent> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
