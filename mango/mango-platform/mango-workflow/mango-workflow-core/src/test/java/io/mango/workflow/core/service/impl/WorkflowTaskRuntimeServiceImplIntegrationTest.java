package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import io.mango.workflow.core.entity.WorkflowTaskRecord;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import io.mango.workflow.core.service.WorkflowTaskAdvanceResult;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.mockito.Answers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskRuntimeServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_runtime_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class WorkflowTaskRuntimeServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorkflowTaskRuntimeServiceImpl service;
    @Autowired
    private WorkflowDefinitionMapper definitionMapper;
    @Autowired
    private WorkflowFormInstanceMapper formInstanceMapper;
    @Autowired
    private WorkflowTaskRecordMapper taskRecordMapper;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private WorkflowCandidateGroupProvider candidateGroupProvider;
    @Autowired
    private CapturingBusinessApplyService workflowBusinessApplyService;
    @Autowired
    private RecordingWorkflowEventPublisher workflowEventPublisher;
    @Autowired
    private OperationLog operationLog;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "anonymous", "default", "USER", "ORG", 100L, "anonymous"));
        reset(taskService, runtimeService, historyService, repositoryService, candidateGroupProvider);
        workflowBusinessApplyService.clear();
        workflowEventPublisher.clear();
        operationLog.clear();
        rebuildTables();
        stubAliveProcess("proc-1", false);
        when(candidateGroupProvider.currentCandidateGroups()).thenReturn(List.of());
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(true));
        when(runtimeService.getVariables("proc-1")).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void saveDraftMergesStoredVariablesAndPersistsFormAndRecordThroughRealMappers() {
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);
        insertFormInstance("proc-1", "{\"existing\":true,\"approvedAmount\":50}", WorkflowInstanceStatus.RUNNING.name());
        SaveWorkflowTaskDraftCommand command = new SaveWorkflowTaskDraftCommand();
        command.setTaskId("task-1");
        command.setComment("先保存");
        command.setVariables(Map.of("approvedAmount", 100));

        service.saveDraft(command);

        WorkflowFormInstance formInstance = findFormInstance("proc-1");
        assertThat(formInstance.getVariablesJson())
                .contains("\"existing\":true")
                .contains("\"approvedAmount\":100");
        assertThat(formInstance.getStatus()).isEqualTo(WorkflowInstanceStatus.RUNNING.name());
        assertThat(records()).singleElement()
                .returns("SAVE", WorkflowTaskRecord::getAction)
                .returns("先保存", WorkflowTaskRecord::getComment)
                .returns(1001L, WorkflowTaskRecord::getOperatorId);
        verify(runtimeService).setVariables("proc-1", Map.of("existing", true, "approvedAmount", 100));
        verify(taskService, never()).complete(eq("task-1"), any());
        assertThat(workflowBusinessApplyService.refreshedProcessInstanceIds()).containsExactly("proc-1");
    }

    @Test
    void transferClaimAndUnclaimPersistRealTaskRecords() {
        insertFormInstance("proc-1", "{}", WorkflowInstanceStatus.RUNNING.name());
        Task assignedTask = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery assignedQuery = taskQuery(assignedTask, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(assignedQuery);
        TransferWorkflowTaskCommand transfer = new TransferWorkflowTaskCommand();
        transfer.setTaskId("task-1");
        transfer.setTargetUserId("lisi");
        transfer.setComment("请李四处理");

        service.transfer(transfer);

        Task unassignedTask = task("task-1", "manager_approve", "proc-1", "pd-1", null);
        TaskQuery unassignedQuery = taskQuery(unassignedTask, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(unassignedQuery);
        ClaimWorkflowTaskCommand claim = new ClaimWorkflowTaskCommand();
        claim.setTaskId("task-1");

        service.claim(claim);

        Task claimedTask = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery claimedQuery = taskQuery(claimedTask, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(claimedQuery);
        when(taskService.getVariableLocal("task-1", "mangoClaimedFromCandidate")).thenReturn(Boolean.TRUE);

        service.unclaim(claim);

        assertThat(records())
                .extracting(WorkflowTaskRecord::getAction)
                .containsExactly("TRANSFER", "CLAIM", "UNCLAIM");
        assertThat(records())
                .extracting(WorkflowTaskRecord::getComment)
                .containsExactly("请李四处理", "认领任务", "释放任务");
        verify(taskService).setAssignee("task-1", "lisi");
        verify(taskService).claim("task-1", "anonymous");
        verify(taskService).unclaim("task-1");
    }

    @Test
    void addSignRejectsCurrentUserByUsernameAndUserIdBeforeEngineMutation() {
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);

        AddSignWorkflowTaskCommand byUsername = new AddSignWorkflowTaskCommand();
        byUsername.setTaskId("task-1");
        byUsername.setTargetUserIds(List.of("anonymous"));
        byUsername.setComment("加签给自己");

        assertThatThrownBy(() -> service.addSign(byUsername))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能加签给自己");

        AddSignWorkflowTaskCommand byUserId = new AddSignWorkflowTaskCommand();
        byUserId.setTaskId("task-1");
        byUserId.setTargetUserIds(List.of("1001"));
        byUserId.setComment("加签给自己");

        assertThatThrownBy(() -> service.addSign(byUserId))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能加签给自己");
        verify(runtimeService, never()).addMultiInstanceExecution(any(), any(), any());
        assertThat(records()).isEmpty();
    }

    @Test
    void completeRejectsDisabledActionOrMissingRequiredCommentBeforePersistence() {
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(false));
        CompleteWorkflowTaskCommand disabled = new CompleteWorkflowTaskCommand();
        disabled.setTaskId("task-1");

        assertThatThrownBy(() -> service.complete(disabled))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前节点未启用该审批动作");

        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(true, true));
        CompleteWorkflowTaskCommand withoutComment = new CompleteWorkflowTaskCommand();
        withoutComment.setTaskId("task-1");
        withoutComment.setComment("   ");

        assertThatThrownBy(() -> service.complete(withoutComment))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请填写审批意见");
        verify(taskService, never()).complete(eq("task-1"), any());
        assertThat(records()).isEmpty();
    }

    @Test
    void completeWithResultPersistsRecordUpdatesFormAndPublishesAdvancedAfterRefresh() {
        insertFormInstance("proc-1", "{\"existing\":true}", WorkflowInstanceStatus.RUNNING.name());
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);
        CompleteWorkflowTaskCommand command = new CompleteWorkflowTaskCommand();
        command.setTaskId("task-1");
        command.setComment("同意");
        command.setVariables(Map.of("approved", true));

        WorkflowTaskCompleteResultVO result = service.completeWithResult(command).getData();

        assertThat(result.getCompletedTaskId()).isEqualTo("task-1");
        assertThat(result.getProcessInstanceId()).isEqualTo("proc-1");
        assertThat(result.getEnded()).isFalse();
        assertThat(result.getApplyId()).isEqualTo(1001L);
        assertThat(result.getApplyStatus()).isEqualTo(WorkflowApplyStatus.IN_APPROVAL);
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("task-2", WorkflowBusinessApplyCurrentTaskVO::getTaskId)
                .returns("supervisor_approve", WorkflowBusinessApplyCurrentTaskVO::getTaskDefinitionKey);
        assertThat(findFormInstance("proc-1").getVariablesJson())
                .contains("\"existing\":true")
                .contains("\"approved\":true");
        assertThat(records()).singleElement()
                .returns("COMPLETE", WorkflowTaskRecord::getAction)
                .returns("同意", WorkflowTaskRecord::getComment);
        assertThat(operationLog.entries()).containsSubsequence(
                "refreshCurrentTasksAndReturn:proc-1",
                "publishTaskAdvanced:proc-1");
    }

    @Test
    void returnTaskMovesToPreviousHistoricNodeAndRefreshesCurrentTasksWithoutEndingProcess() {
        insertFormInstance("proc-1", "{\"existing\":true}", WorkflowInstanceStatus.RUNNING.name());
        Task currentTask = task("task-2", "supervisor_approve", "proc-1", "pd-1", "anonymous");
        Task returnedTask = task("task-3", "risk_specialist_review", "proc-1", "pd-1", null);
        TaskQuery query = taskQuery(currentTask, 1L, List.of(returnedTask));
        when(taskService.createTaskQuery()).thenReturn(query);
        HistoricTaskInstance historicTask = mock(HistoricTaskInstance.class);
        when(historicTask.getTaskDefinitionKey()).thenReturn("risk_specialist_review");
        HistoricTaskInstanceQuery historicTaskQuery = mock(HistoricTaskInstanceQuery.class, Answers.RETURNS_SELF);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTaskQuery);
        when(historicTaskQuery.list()).thenReturn(List.of(historicTask));
        ChangeActivityStateBuilder stateBuilder = mock(ChangeActivityStateBuilder.class, Answers.RETURNS_SELF);
        when(runtimeService.createChangeActivityStateBuilder()).thenReturn(stateBuilder);
        workflowBusinessApplyService.nextCurrentTask("task-3", "risk_specialist_review", "风控专员初审", "risk-specialist");
        ReturnWorkflowTaskCommand command = new ReturnWorkflowTaskCommand();
        command.setTaskId("task-2");
        command.setComment("退回专员补充材料");
        command.setVariables(Map.of("returnReason", "缺少附件"));

        WorkflowTaskCompleteResultVO result = service.returnTask(command).getData();

        verify(stateBuilder).processInstanceId("proc-1");
        verify(stateBuilder).moveActivityIdTo("supervisor_approve", "risk_specialist_review");
        verify(stateBuilder).changeState();
        verify(runtimeService, never()).deleteProcessInstance(any(), any());
        assertThat(result.getEnded()).isFalse();
        assertThat(result.getCurrentTasks()).singleElement()
                .returns("task-3", WorkflowBusinessApplyCurrentTaskVO::getTaskId)
                .returns("risk_specialist_review", WorkflowBusinessApplyCurrentTaskVO::getTaskDefinitionKey);
        assertThat(records()).singleElement()
                .returns("RETURN", WorkflowTaskRecord::getAction)
                .returns("退回专员补充材料", WorkflowTaskRecord::getComment)
                .satisfies(record -> assertThat(record.getVariablesJson()).contains("risk_specialist_review"));
        assertThat(operationLog.entries()).containsSubsequence(
                "refreshCurrentTasksAndReturn:proc-1",
                "publishTaskAdvanced:proc-1");
    }

    @Test
    void detailReturnsBusinessErrorWhenProcessDefinitionMissing() {
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);
        when(repositoryService.getBpmnModel("pd-1"))
                .thenThrow(new FlowableObjectNotFoundException("no deployed process definition found"));

        assertThatThrownBy(() -> service.detail("task-1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前任务引用的流程定义不存在");
    }

    @Test
    void detailFallsBackToDefinitionFormAndReadonlyPermissionsThroughRealMapper() {
        Task task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task, 1L, List.of());
        when(taskService.createTaskQuery()).thenReturn(query);
        when(runtimeService.getVariables("proc-1"))
                .thenReturn(Map.of("mangoDefinitionId", 1001L, "reason", "请假"));
        insertDefinition(1001L, "pd-1", "claim_form", """
                {"rules":[
                  {"type":"input","field":"title","title":"标题"},
                  {"type":"textarea","field":"reason","title":"请假原因"}
                ]}
                """);

        WorkflowTaskDetailVO detail = service.detail("task-1").getData();

        assertThat(detail.getFormCode()).isEqualTo("claim_form");
        assertThat(detail.getFormJson()).contains("请假原因");
        assertThat(detail.getVariables()).containsEntry("reason", "请假");
        assertThat(detail.getFormPermissions())
                .containsEntry("title", "READONLY")
                .containsEntry("reason", "READONLY");
        assertThat(detail.getRenderConfig().getFormPermissions())
                .containsEntry("title", "READONLY")
                .containsEntry("reason", "READONLY");
    }

    @Test
    void myTaskSummaryAggregatesCurrentUserTaskStatus() {
        TaskQuery pendingQuery = countTaskQuery(3L);
        TaskQuery processingQuery = countTaskQuery(5L);
        TaskQuery overdueAssignedQuery = listTaskQuery(List.of(task("overdue-1", "node", "proc-1", "pd-1", "anonymous")));
        TaskQuery overdueClaimableQuery = listTaskQuery(List.of(task("overdue-2", "node", "proc-2", "pd-1", null)));
        HistoricTaskInstanceQuery completedQuery = mock(HistoricTaskInstanceQuery.class);
        when(taskService.createTaskQuery())
                .thenReturn(pendingQuery, processingQuery, overdueAssignedQuery, overdueClaimableQuery);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(completedQuery);
        when(completedQuery.taskAssignee("anonymous")).thenReturn(completedQuery);
        when(completedQuery.finished()).thenReturn(completedQuery);
        when(completedQuery.count()).thenReturn(7L);

        WorkflowMyTaskSummaryVO summary = service.myTaskSummary().getData();

        assertThat(summary.getPending()).isEqualTo(3L);
        assertThat(summary.getProcessing()).isEqualTo(5L);
        assertThat(summary.getCompleted()).isEqualTo(7L);
        assertThat(summary.getOverdue()).isEqualTo(2L);
        assertThat(summary.getTotal()).isEqualTo(17L);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists workflow_copied_task");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_form_instance");
        jdbcTemplate.execute("drop table if exists workflow_definition");
        jdbcTemplate.execute("""
                create table workflow_definition (
                    id bigint not null,
                    tenant_id bigint,
                    category_id bigint,
                    domain_code varchar(64),
                    org_id bigint,
                    admin_users text,
                    start_entry_visible boolean,
                    icon varchar(512),
                    definition_name varchar(128),
                    definition_key varchar(128),
                    deployment_id varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    published_version_no int,
                    source_template_id bigint,
                    source_template_code varchar(128),
                    source_template_version int,
                    designer_json text,
                    bpmn_xml text,
                    form_code varchar(128),
                    form_json text,
                    status varchar(64),
                    last_deploy_time timestamp,
                    remark varchar(255),
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
                create table workflow_form_instance (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    business_key varchar(128),
                    definition_id bigint,
                    definition_key varchar(128),
                    definition_name varchar(128),
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
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_name varchar(255),
                    task_definition_key varchar(128),
                    action varchar(64),
                    action_name varchar(64),
                    operator_id bigint,
                    operator_name varchar(128),
                    comment text,
                    variables_json text,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    process_instance_id varchar(128),
                    business_key varchar(128),
                    apply_title varchar(255),
                    process_name varchar(128),
                    current_task_names varchar(255),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_copied_task (
                    id bigint not null,
                    copied_user_id varchar(128),
                    read_flag boolean,
                    created_time timestamp,
                    primary key (id)
                )
                """);
    }

    private void insertFormInstance(String processInstanceId, String variablesJson, String status) {
        WorkflowFormInstance formInstance = new WorkflowFormInstance();
        formInstance.setId(2001L);
        formInstance.setTenantId(1L);
        formInstance.setProcessInstanceId(processInstanceId);
        formInstance.setBusinessKey("BIZ-1");
        formInstance.setDefinitionId(1001L);
        formInstance.setDefinitionKey("test_process");
        formInstance.setDefinitionName("测试流程");
        formInstance.setProcessDefinitionId("pd-1");
        formInstance.setProcessDefinitionVersion(1);
        formInstance.setFormCode("form_leave");
        formInstance.setFormJson("[{\"type\":\"textarea\",\"field\":\"reason\",\"title\":\"请假原因\"}]");
        formInstance.setVariablesJson(variablesJson);
        formInstance.setStatus(status);
        LocalDateTime now = LocalDateTime.parse("2026-06-27T10:00:00");
        formInstance.setCreatedBy(1001L);
        formInstance.setCreatedTime(now);
        formInstance.setCreatedAt(now);
        formInstance.setUpdatedBy(1001L);
        formInstance.setUpdatedTime(now);
        formInstance.setUpdatedAt(now);
        assertThat(formInstanceMapper.insert(formInstance)).isEqualTo(1);
    }

    private WorkflowFormInstance findFormInstance(String processInstanceId) {
        return formInstanceMapper.selectOne(new QueryWrapper<WorkflowFormInstance>()
                .eq("process_instance_id", processInstanceId)
                .last("limit 1"));
    }

    private List<WorkflowTaskRecord> records() {
        return taskRecordMapper.selectList(new QueryWrapper<WorkflowTaskRecord>()
                .orderByAsc("created_time")
                .orderByAsc("id"));
    }

    private void insertDefinition(Long id, String processDefinitionId, String formCode, String formJson) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(id);
        definition.setTenantId(1L);
        definition.setDefinitionName("测试流程");
        definition.setDefinitionKey("test_process");
        definition.setProcessDefinitionId(processDefinitionId);
        definition.setProcessDefinitionVersion(1);
        definition.setFormCode(formCode);
        definition.setFormJson(formJson);
        definition.setCreatedBy(1001L);
        definition.setCreatedTime(LocalDateTime.parse("2026-06-27T10:00:00"));
        definition.setCreatedAt(LocalDateTime.parse("2026-06-27T10:00:00"));
        assertThat(definitionMapper.insert(definition)).isEqualTo(1);
    }

    private void stubAliveProcess(String processInstanceId, boolean ended) {
        HistoricProcessInstance historicInstance = mock(HistoricProcessInstance.class);
        when(historicInstance.getId()).thenReturn(processInstanceId);
        when(historicInstance.getBusinessKey()).thenReturn("BIZ-1");
        when(historicInstance.getProcessDefinitionId()).thenReturn("pd-1");
        when(historicInstance.getProcessDefinitionName()).thenReturn("测试流程");
        when(historicInstance.getProcessDefinitionKey()).thenReturn("test_process");
        when(historicInstance.getEndTime()).thenReturn(ended ? java.sql.Timestamp.valueOf("2026-06-27 10:00:00") : null);
        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId(processInstanceId)).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historicInstance);
        HistoricVariableInstanceQuery variableQuery = mock(HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(variableQuery);
        when(variableQuery.processInstanceId(processInstanceId)).thenReturn(variableQuery);
        when(variableQuery.variableName("mangoInitiatorName")).thenReturn(variableQuery);
        when(variableQuery.singleResult()).thenReturn(null);
        ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.processInstanceId(processInstanceId)).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(mock(ProcessInstance.class));
    }

    private Task task(String id, String definitionKey, String processInstanceId, String processDefinitionId, String assignee) {
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(id);
        when(mockTask.getName()).thenReturn("经理审批");
        when(mockTask.getTaskDefinitionKey()).thenReturn(definitionKey);
        when(mockTask.getProcessInstanceId()).thenReturn(processInstanceId);
        when(mockTask.getProcessDefinitionId()).thenReturn(processDefinitionId);
        when(mockTask.getAssignee()).thenReturn(assignee);
        return mockTask;
    }

    private TaskQuery taskQuery(Task result, long count, List<Task> list) {
        TaskQuery query = countTaskQuery(count);
        when(query.taskId(any())).thenReturn(query);
        when(query.singleResult()).thenReturn(result);
        when(query.processInstanceId(any())).thenReturn(query);
        when(query.list()).thenReturn(list);
        return query;
    }

    private TaskQuery countTaskQuery(long count) {
        TaskQuery query = mock(TaskQuery.class, Answers.RETURNS_SELF);
        when(query.count()).thenReturn(count);
        return query;
    }

    private TaskQuery listTaskQuery(List<Task> tasks) {
        TaskQuery query = countTaskQuery(tasks.size());
        when(query.taskDueBefore(any())).thenReturn(query);
        when(query.list()).thenReturn(tasks);
        return query;
    }

    private BpmnModel bpmnModel(boolean completeEnabled) {
        return bpmnModel(completeEnabled, false);
    }

    private BpmnModel bpmnModel(boolean completeEnabled, boolean completeRequireComment) {
        BpmnModel model = new BpmnModel();
        UserTask userTask = new UserTask();
        userTask.setId("manager_approve");
        ExtensionElement element = new ExtensionElement();
        element.setName("mangoApprovalConfig");
        element.setElementText("""
                {"actions":{"complete":{"enabled":%s,"requireComment":%s},"save":{"enabled":true},"transfer":{"enabled":true},"addSign":{"enabled":true},"reject":{"enabled":true},"returnTask":{"enabled":true}}}
                """.formatted(completeEnabled, completeRequireComment));
        userTask.getExtensionElements().put("mangoApprovalConfig", List.of(element));
        UserTask supervisorTask = new UserTask();
        supervisorTask.setId("supervisor_approve");
        supervisorTask.getExtensionElements().put("mangoApprovalConfig", List.of(element));
        Process process = new Process();
        process.addFlowElement(userTask);
        process.addFlowElement(supervisorTask);
        model.addProcess(process);
        return model;
    }

    @Configuration
    @Import(WorkflowTaskRuntimeServiceImpl.class)
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

        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        HistoryService historyService() {
            return mock(HistoryService.class);
        }

        @Bean
        RepositoryService repositoryService() {
            return mock(RepositoryService.class);
        }

        @Bean
        WorkflowAssigneeResolver workflowAssigneeResolver() {
            return mock(WorkflowAssigneeResolver.class);
        }

        @Bean
        WorkflowCandidateGroupProvider workflowCandidateGroupProvider() {
            return mock(WorkflowCandidateGroupProvider.class);
        }

        @Bean
        OperationLog operationLog() {
            return new OperationLog();
        }

        @Bean
        RecordingWorkflowEventPublisher workflowEventPublisher(ObjectProvider<IDomainEventPublisher> provider,
                                                               OperationLog operationLog) {
            return new RecordingWorkflowEventPublisher(provider, operationLog);
        }

        @Bean
        CapturingBusinessApplyService workflowBusinessApplyService(OperationLog operationLog) {
            return new CapturingBusinessApplyService(operationLog);
        }
    }

    static class OperationLog {
        private final List<String> entries = new ArrayList<>();

        void add(String entry) {
            entries.add(entry);
        }

        List<String> entries() {
            return List.copyOf(entries);
        }

        void clear() {
            entries.clear();
        }
    }

    static class RecordingWorkflowEventPublisher extends WorkflowEventPublisher {
        private final OperationLog operationLog;

        RecordingWorkflowEventPublisher(ObjectProvider<IDomainEventPublisher> provider, OperationLog operationLog) {
            super(provider);
            this.operationLog = operationLog;
        }

        void clear() {
        }

        @Override
        public void publishTaskAdvanced(Task completedTask, WorkflowFormInstance formInstance, Map<String, Object> variables,
                                        String comment, boolean ended, WorkflowBusinessApplyVO businessApply) {
            operationLog.add("publishTaskAdvanced:" + completedTask.getProcessInstanceId());
        }
    }

    static class CapturingBusinessApplyService implements IWorkflowBusinessApplyService {
        private final OperationLog operationLog;
        private final List<String> refreshedProcessInstanceIds = new ArrayList<>();
        private WorkflowBusinessApplyCurrentTaskVO nextCurrentTask;

        CapturingBusinessApplyService(OperationLog operationLog) {
            this.operationLog = operationLog;
        }

        void clear() {
            refreshedProcessInstanceIds.clear();
            nextCurrentTask = null;
        }

        List<String> refreshedProcessInstanceIds() {
            return List.copyOf(refreshedProcessInstanceIds);
        }

        void nextCurrentTask(String taskId, String taskDefinitionKey, String taskName, String assigneeName) {
            WorkflowBusinessApplyCurrentTaskVO currentTask = new WorkflowBusinessApplyCurrentTaskVO();
            currentTask.setTaskId(taskId);
            currentTask.setTaskDefinitionKey(taskDefinitionKey);
            currentTask.setTaskName(taskName);
            currentTask.setAssigneeName(assigneeName);
            nextCurrentTask = currentTask;
        }

        @Override
        public R<WorkflowBusinessApplyVO> create(CreateWorkflowBusinessApplyCommand command) {
            return R.ok(null);
        }

        @Override
        public void markProcessStarted(Long applyId, Long processDefinitionId, String processDefinitionKey,
                                       String engineProcessDefinitionId, String processName, String processInstanceId) {
        }

        @Override
        public R<PageResult<WorkflowBusinessApplyVO>> page(WorkflowBusinessApplyPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<WorkflowBusinessApplySummaryVO> mySummary() {
            return R.ok(new WorkflowBusinessApplySummaryVO());
        }

        @Override
        public R<WorkflowBusinessApplyVO> detail(Long applyId) {
            return R.ok(null);
        }

        @Override
        public R<PageResult<WorkflowBusinessApplyVO>> history(String businessType, String businessKey,
                                                              WorkflowBusinessApplyPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<WorkflowBusinessApplyProgressVO> latestProgress(String businessType, String businessKey) {
            return R.ok(null);
        }

        @Override
        public Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType,
                                                                           Collection<String> businessKeys) {
            return Map.of();
        }

        @Override
        public List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType,
                                                                  Collection<String> businessKeys) {
            return List.of();
        }

        @Override
        public R<WorkflowBusinessApplyVO> byProcessInstance(String processInstanceId) {
            return R.ok(findByProcessInstance(processInstanceId));
        }

        @Override
        public WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId) {
            return null;
        }

        @Override
        public void refreshCurrentTasks(String processInstanceId) {
            refreshedProcessInstanceIds.add(processInstanceId);
        }

        @Override
        public WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId) {
            operationLog.add("refreshCurrentTasksAndReturn:" + processInstanceId);
            return advancedApply();
        }

        @Override
        public void markApproved(String processInstanceId) {
        }

        @Override
        public void markRejected(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        }

        @Override
        public void markTerminated(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        }

        private WorkflowBusinessApplyVO advancedApply() {
            WorkflowBusinessApplyCurrentTaskVO currentTask = nextCurrentTask;
            if (currentTask == null) {
                currentTask = new WorkflowBusinessApplyCurrentTaskVO();
                currentTask.setTaskId("task-2");
                currentTask.setTaskDefinitionKey("supervisor_approve");
                currentTask.setTaskName("主管审批");
                currentTask.setAssigneeName("supervisor");
            }
            WorkflowBusinessApplyVO apply = new WorkflowBusinessApplyVO();
            apply.setId(1001L);
            apply.setBusinessType("GUARANTEE_RISK_REVIEW");
            apply.setBusinessKey("APP-1");
            apply.setApplyStatus(WorkflowApplyStatus.IN_APPROVAL);
            apply.setApplyStatusName("审批中");
            apply.setCurrentTaskNames(currentTask.getTaskName());
            apply.setCurrentTaskDefinitionKeys(currentTask.getTaskDefinitionKey());
            apply.setCurrentAssigneeNames(currentTask.getAssigneeName());
            apply.setCurrentTasks(List.of(currentTask));
            return apply;
        }
    }
}
