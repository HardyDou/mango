package io.mango.workflow.core.service.impl;

import io.mango.workflow.core.model.WorkflowProcessStartedContext;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.workflow.api.WorkflowEventTypes;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.WorkflowJsonRequest;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyCurrentTaskMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.mapper.WorkflowAutoAssignmentStateMapper;
import io.mango.workflow.core.mapper.WorkflowCopiedTaskMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowParticipationService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private WorkflowAssigneeResolver assigneeResolver;
    @Autowired
    private CollectingDomainEventPublisher eventPublisher;
    @Autowired
    private TenantMemberProvider tenantMemberProvider;

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
        StartedWorkflow started = startIssue233Workflow("BIZ-233");

        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        assertThat(managerTask).isNotNull();

        CompleteWorkflowTaskCommand command = new CompleteWorkflowTaskCommand();
        command.setTaskId(managerTask.getId());
        command.setComment("同意");
        var result = taskRuntimeService.completeWithResult(command);

        assertThat(result.getEnded()).isFalse();
        assertThat(result.getApplyId()).isEqualTo(started.applyId());
        assertThat(result.getApplyStatus()).isEqualTo(WorkflowApplyStatus.IN_APPROVAL);
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("finance_approve", task -> task.getTaskDefinitionKey())
                .returns("finance", task -> task.getAssigneeName());

        assertThat(currentTaskDefinitionKeys(started.applyId())).containsExactly("finance_approve");

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
                .containsEntry("applyId", started.applyId())
                .containsEntry("currentTaskDefinitionKeys", "finance_approve")
                .containsEntry("currentAssigneeNames", "finance");
        assertThat((List<?>) advancedEvent.getPayload().get("currentTasks")).singleElement()
                .satisfies(currentTask -> assertThat(((Map<?, ?>) currentTask).get("taskDefinitionKey"))
                        .isEqualTo("finance_approve"));
    }

    @Test
    void returnTaskMovesRuntimeExecutionBackToFinishedUserTaskAndRefreshesCurrentTasks() {
        StartedWorkflow started = startIssue233Workflow("BIZ-296");
        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        CompleteWorkflowTaskCommand completeCommand = new CompleteWorkflowTaskCommand();
        completeCommand.setTaskId(managerTask.getId());
        completeCommand.setComment("提交财务");
        taskRuntimeService.completeWithResult(completeCommand);
        eventPublisher.clear();

        Task financeTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .singleResult();
        assertThat(financeTask).isNotNull();
        switchUser(1002L, "finance");
        ReturnWorkflowTaskCommand returnCommand = new ReturnWorkflowTaskCommand();
        returnCommand.setTaskId(financeTask.getId());
        returnCommand.setComment("退回经理补充");
        var result = taskRuntimeService.returnTask(returnCommand);

        assertThat(result.getEnded()).isFalse();
        assertThat(result.getApplyId()).isEqualTo(started.applyId());
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("manager_approve", task -> task.getTaskDefinitionKey())
                .returns("admin", task -> task.getAssigneeName());
        assertThat(currentTaskDefinitionKeys(started.applyId())).containsExactly("manager_approve");
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .count()).isZero();

        DomainEvent advancedEvent = eventPublisher.events().stream()
                .filter(event -> WorkflowEventTypes.TASK_ADVANCED.equals(event.getEventType()))
                .findFirst()
                .orElseThrow();
        assertThat(advancedEvent.getPayload())
                .containsEntry("completedTaskDefinitionKey", "finance_approve")
                .containsEntry("ended", false)
                .containsEntry("applyId", started.applyId())
                .containsEntry("currentTaskDefinitionKeys", "manager_approve")
                .containsEntry("currentAssigneeNames", "admin");
    }

    @Test
    void returnTaskAssignsSpecifiedTargetAssigneeToNewRuntimeTask() {
        StartedWorkflow started = startIssue233Workflow("BIZ-1191-ASSIGN");
        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        CompleteWorkflowTaskCommand completeCommand = new CompleteWorkflowTaskCommand();
        completeCommand.setTaskId(managerTask.getId());
        completeCommand.setComment("提交财务");
        taskRuntimeService.completeWithResult(completeCommand);

        Task financeTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .singleResult();
        switchUser(1002L, "finance");
        when(tenantMemberProvider.getEnabledMember(2001L, 1L)).thenReturn(member(2001L));
        ReturnWorkflowTaskCommand returnCommand = new ReturnWorkflowTaskCommand();
        returnCommand.setTaskId(financeTask.getId());
        returnCommand.setTargetAssigneeUserId("2001");
        returnCommand.setComment("退回原经理");

        var result = taskRuntimeService.returnTask(returnCommand);

        Task returnedTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        assertThat(returnedTask).isNotNull();
        assertThat(returnedTask.getAssignee()).isEqualTo("2001");
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("manager_approve", task -> task.getTaskDefinitionKey())
                .returns("2001", task -> task.getAssigneeName());
        assertThat(currentTaskDefinitionKeys(started.applyId())).containsExactly("manager_approve");
    }

    @Test
    void returnTaskRejectsInvalidTargetAssigneeWithoutMovingRuntimeTask() {
        StartedWorkflow started = startIssue233Workflow("BIZ-1191-INVALID");
        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        CompleteWorkflowTaskCommand completeCommand = new CompleteWorkflowTaskCommand();
        completeCommand.setTaskId(managerTask.getId());
        completeCommand.setComment("提交财务");
        taskRuntimeService.completeWithResult(completeCommand);

        Task financeTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .singleResult();
        switchUser(1002L, "finance");
        ReturnWorkflowTaskCommand returnCommand = new ReturnWorkflowTaskCommand();
        returnCommand.setTaskId(financeTask.getId());
        returnCommand.setTargetAssigneeUserId("9999");
        returnCommand.setComment("退回原经理");

        assertThatThrownBy(() -> taskRuntimeService.returnTask(returnCommand))
                .isInstanceOf(io.mango.common.exception.BizException.class)
                .hasMessageContaining("退回目标办理人不是当前租户的启用成员");
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve").count()).isZero();
    }

    private TenantMemberVO member(Long userId) {
        TenantMemberVO member = new TenantMemberVO();
        member.setMemberId(userId);
        member.setTenantId(1L);
        member.setUserId(userId);
        member.setStatus(1);
        return member;
    }

    @Test
    void returnTaskWithExplicitTargetAndClientVariablesMovesRuntimeTask() {
        StartedWorkflow started = startIssue233Workflow("BIZ-1179");
        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .singleResult();
        CompleteWorkflowTaskCommand completeCommand = new CompleteWorkflowTaskCommand();
        completeCommand.setTaskId(managerTask.getId());
        completeCommand.setComment("提交财务");
        taskRuntimeService.completeWithResult(completeCommand);

        Task financeTask = taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .singleResult();
        switchUser(1002L, "finance");
        ReturnWorkflowTaskCommand returnCommand = new ReturnWorkflowTaskCommand();
        returnCommand.setTaskId(financeTask.getId());
        returnCommand.setTargetTaskDefinitionKey("manager_approve");
        returnCommand.setComment("退回");
        returnCommand.setVariables(io.mango.workflow.api.command.WorkflowJsonRequest.of(Map.of(
                "targetTaskDefinitionKey", "manager_approve",
                "decision", "RETURN")));

        var result = taskRuntimeService.returnTask(returnCommand);

        assertThat(result.getActionResult()).isEqualTo(io.mango.workflow.api.enums.WorkflowTaskAction.RETURN);
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("manager_approve", task -> task.getTaskDefinitionKey());
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("manager_approve")
                .count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery()
                .processInstanceId(started.processInstanceId())
                .taskDefinitionKey("finance_approve")
                .count()).isZero();
    }

    @Test
    void returnTaskMovesRuntimeTaskOnBusinessRiskReviewDesignerModel() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        WorkflowDesignerBpmnConverter converter = new WorkflowDesignerBpmnConverter(objectMapper, assigneeResolver);
        String designerJson;
        try (var stream = java.util.Objects.requireNonNull(
                getClass().getResourceAsStream("/workflow/issue-1179-risk-review-designer.json"))) {
            designerJson = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        String processKey = "WF_GUARANTEE_RISK_REVIEW";
        repositoryService.createDeployment()
                .name("issue-1179-risk-review")
                .addBpmnModel(processKey + ".bpmn20.xml", converter.toModel(designerJson, processKey, "保函风控审核"))
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey).latestVersion().singleResult();

        CreateWorkflowBusinessApplyCommand applyCommand = new CreateWorkflowBusinessApplyCommand();
        applyCommand.setBusinessType("GUARANTEE");
        applyCommand.setBusinessKey("BIZ-1179-RISK");
        applyCommand.setApplyTitle("Issue 1179 业务流程复现");
        applyCommand.setProcessDefinitionKey(processKey);
        Long applyId = businessApplyService.create(applyCommand).getId();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, "BIZ-1179-RISK", Map.of(
                "businessType", "GUARANTEE",
                "businessKey", "BIZ-1179-RISK",
                "applyId", String.valueOf(applyId),
                "mangoInitiator", "admin",
                "submitManager", "是",
                "managerReviewRequired", true
        ));
        businessApplyService.markProcessStarted(new WorkflowProcessStartedContext(
                applyId, 1179L, processKey, processDefinition.getId(), processDefinition.getName(),
                instance.getProcessInstanceId()));

        Task specialistTask = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey("risk_specialist_review")
                .singleResult();
        assertThat(specialistTask).isNotNull();
        CompleteWorkflowTaskCommand completeCommand = new CompleteWorkflowTaskCommand();
        completeCommand.setTaskId(specialistTask.getId());
        completeCommand.setComment("初审通过");
        taskRuntimeService.completeWithResult(completeCommand);

        Task supervisorTask = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey("risk_supervisor_review")
                .singleResult();
        assertThat(supervisorTask).isNotNull();
        ReturnWorkflowTaskCommand returnCommand = new ReturnWorkflowTaskCommand();
        returnCommand.setTaskId(supervisorTask.getId());
        returnCommand.setTargetTaskDefinitionKey("risk_specialist_review");
        returnCommand.setComment("退回专员");
        returnCommand.setVariables(WorkflowJsonRequest.of(Map.of(
                "targetTaskDefinitionKey", "risk_specialist_review",
                "decision", "RETURN")));

        var result = taskRuntimeService.returnTask(returnCommand);

        assertThat(result.getActionResult()).isEqualTo(WorkflowTaskAction.RETURN);
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("risk_specialist_review", task -> task.getTaskDefinitionKey());
        assertThat(taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey("risk_specialist_review").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey("risk_supervisor_review").count()).isZero();
        assertThat(currentTaskDefinitionKeys(applyId)).containsExactly("risk_specialist_review");
    }

    private void switchUser(Long userId, String principalName) {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(userId, "1", principalName, "default", "USER", "ORG", userId, principalName));
    }

    private StartedWorkflow startIssue233Workflow(String businessKey) {
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
        applyCommand.setBusinessKey(businessKey);
        applyCommand.setApplyTitle("Issue 233 集成测试");
        applyCommand.setProcessDefinitionKey("issue_233_runtime");
        Long applyId = businessApplyService.create(applyCommand).getId();

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("issue_233_runtime", businessKey, Map.of(
                "businessType", "ISSUE_233",
                "businessKey", businessKey,
                "applyId", String.valueOf(applyId),
                "mangoInitiator", "admin"
        ));
        businessApplyService.markProcessStarted(new WorkflowProcessStartedContext(
                applyId,
                233L,
                "issue_233_runtime",
                processDefinition.getId(),
                processDefinition.getName(),
                instance.getProcessInstanceId()));
        return new StartedWorkflow(applyId, instance.getProcessInstanceId());
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
                    task_definition_key varchar(255),
                    task_name varchar(255),
                    assignee_id bigint,
                    assignee_name varchar(128),
                    claim_status varchar(32) not null default 'ASSIGNED',
                    candidate_users varchar(1000),
                    candidate_groups varchar(1000),
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
                    task_definition_key varchar(255),
                    process_instance_id varchar(128),
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
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
                    created_by bigint,
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
                    org_id bigint,
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
                    org_id bigint,
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
                    created_by bigint,
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
                        <mango:mangoApprovalConfig><![CDATA[{"actions":{"complete":{"enabled":true},"returnTask":{"enabled":true,"requireComment":true}}}]]></mango:mangoApprovalConfig>
                      </extensionElements>
                    </userTask>
                    <sequenceFlow id="flow_finance_end" sourceRef="finance_approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """;
    }

    private record StartedWorkflow(Long applyId, String processInstanceId) {
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
            WorkflowAssigneeIdentityService.class,
            WorkflowEventPublisher.class,
            WorkflowBusinessApplyService.class,
            WorkflowTaskRuntimeService.class
    })
    @MapperScan(basePackageClasses = {
            WorkflowBusinessApplyMapper.class,
            WorkflowBusinessApplyCurrentTaskMapper.class,
            WorkflowBusinessApplyStatusLogMapper.class,
            WorkflowAutoAssignmentStateMapper.class,
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

        @Bean
        IWorkflowParticipationService workflowParticipationService() {
            return mock(IWorkflowParticipationService.class);
        }

        @Bean
        AuthUserProvider authUserProvider() {
            return mock(AuthUserProvider.class);
        }

        @Bean
        TenantMemberProvider tenantMemberProvider() {
            return mock(TenantMemberProvider.class);
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
