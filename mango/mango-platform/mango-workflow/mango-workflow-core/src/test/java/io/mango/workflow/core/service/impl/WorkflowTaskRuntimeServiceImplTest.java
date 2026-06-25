package io.mango.workflow.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowCopiedTaskMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowTaskRuntimeServiceImplTest {

    @Mock
    private TaskService taskService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private HistoryService historyService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private WorkflowBusinessApplyMapper businessApplyMapper;
    @Mock
    private WorkflowCopiedTaskMapper copiedTaskMapper;
    @Mock
    private WorkflowDefinitionMapper definitionMapper;
    @Mock
    private WorkflowFormInstanceMapper formInstanceMapper;
    @Mock
    private WorkflowTaskRecordMapper taskRecordMapper;
    @Mock
    private WorkflowAssigneeResolver assigneeResolver;
    @Mock
    private WorkflowCandidateGroupProvider candidateGroupProvider;
    @Mock
    private IWorkflowBusinessApplyService workflowBusinessApplyService;
    @Mock
    private WorkflowEventPublisher workflowEventPublisher;

    private WorkflowTaskRuntimeServiceImpl service;
    private Task task;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        MockitoAnnotations.openMocks(this);
        service = new WorkflowTaskRuntimeServiceImpl(
                taskService,
                runtimeService,
                historyService,
                repositoryService,
                businessApplyMapper,
                copiedTaskMapper,
                definitionMapper,
                formInstanceMapper,
                taskRecordMapper,
                new ObjectMapper(),
                assigneeResolver,
                candidateGroupProvider,
                workflowBusinessApplyService,
                workflowEventPublisher);
        task = task("task-1", "manager_approve", "proc-1", "pd-1", "anonymous");
        TaskQuery query = taskQuery(task);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(candidateGroupProvider.currentCandidateGroups()).thenReturn(List.of());
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(true));
        ProcessInstanceQuery processInstanceQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("proc-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(mock(ProcessInstance.class));
        when(runtimeService.getVariables("proc-1")).thenReturn(Map.of());
    }

    @Test
    void saveDraft_shouldPersistVariablesWithoutCompletingTask() {
        SaveWorkflowTaskDraftCommand command = new SaveWorkflowTaskDraftCommand();
        command.setTaskId("task-1");
        command.setComment("先保存");
        command.setVariables(Map.of("approvedAmount", 100));

        service.saveDraft(command);

        verify(runtimeService).setVariables("proc-1", Map.of("approvedAmount", 100));
        verify(formInstanceMapper).update(eq(null), any());
        verify(taskRecordMapper).insert(argThatRecord(WorkflowTaskAction.SAVE.name(), "先保存"));
        verify(taskService, never()).complete(eq("task-1"), any());
        verify(workflowBusinessApplyService).refreshCurrentTasks("proc-1");
    }

    @Test
    void transfer_shouldChangeAssigneeAndWriteRecord() {
        TransferWorkflowTaskCommand command = new TransferWorkflowTaskCommand();
        command.setTaskId("task-1");
        command.setTargetUserId("lisi");
        command.setComment("请李四处理");

        service.transfer(command);

        verify(taskService).setAssignee("task-1", "lisi");
        verify(taskRecordMapper).insert(argThatRecord(WorkflowTaskAction.TRANSFER.name(), "请李四处理"));
        verify(workflowBusinessApplyService).refreshCurrentTasks("proc-1");
    }

    @Test
    void addSign_shouldRejectCurrentUserByUsername() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "default", "admin", "default", "USER", "USER", 1001L, "internal-admin"));
        task = task("task-1", "manager_approve", "proc-1", "pd-1", "admin");
        TaskQuery query = taskQuery(task);
        when(taskService.createTaskQuery()).thenReturn(query);
        AddSignWorkflowTaskCommand command = new AddSignWorkflowTaskCommand();
        command.setTaskId("task-1");
        command.setTargetUserIds(List.of("admin"));
        command.setComment("加签给自己");

        assertThatThrownBy(() -> service.addSign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能加签给自己");
        verify(runtimeService, never()).addMultiInstanceExecution(any(), any(), any());
    }

    @Test
    void addSign_shouldRejectCurrentUserByUserId() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "default", "admin", "default", "USER", "USER", 1001L, "internal-admin"));
        task = task("task-1", "manager_approve", "proc-1", "pd-1", "admin");
        TaskQuery query = taskQuery(task);
        when(taskService.createTaskQuery()).thenReturn(query);
        AddSignWorkflowTaskCommand command = new AddSignWorkflowTaskCommand();
        command.setTaskId("task-1");
        command.setTargetUserIds(List.of("1001"));
        command.setComment("加签给自己");

        assertThatThrownBy(() -> service.addSign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能加签给自己");
        verify(runtimeService, never()).addMultiInstanceExecution(any(), any(), any());
    }

    @Test
    void claim_shouldClaimCandidateTask() {
        task = task("task-1", "manager_approve", "proc-1", "pd-1", null);
        TaskQuery findQuery = taskQuery(task);
        TaskQuery operateQuery = canOperateQuery(true);
        when(taskService.createTaskQuery()).thenReturn(findQuery, operateQuery);
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(true));
        ClaimWorkflowTaskCommand command = new ClaimWorkflowTaskCommand();
        command.setTaskId("task-1");

        service.claim(command);

        verify(taskService).claim("task-1", "anonymous");
        verify(taskService).setVariableLocal("task-1", "mangoClaimedFromCandidate", Boolean.TRUE);
        verify(taskRecordMapper).insert(argThatRecord(WorkflowTaskAction.CLAIM.name(), "认领任务"));
    }

    @Test
    void unclaim_shouldRejectDirectAssignedTask() {
        ClaimWorkflowTaskCommand command = new ClaimWorkflowTaskCommand();
        command.setTaskId("task-1");
        when(taskService.getVariableLocal("task-1", "mangoClaimedFromCandidate")).thenReturn(null);

        assertThatThrownBy(() -> service.unclaim(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能释放通过认领获得的任务");
        verify(taskService, never()).unclaim("task-1");
    }

    @Test
    void unclaim_shouldReleaseClaimedCandidateTask() {
        ClaimWorkflowTaskCommand command = new ClaimWorkflowTaskCommand();
        command.setTaskId("task-1");
        when(taskService.getVariableLocal("task-1", "mangoClaimedFromCandidate")).thenReturn(Boolean.TRUE);

        service.unclaim(command);

        verify(taskService).unclaim("task-1");
        verify(taskService).removeVariableLocal("task-1", "mangoClaimedFromCandidate");
        verify(taskRecordMapper).insert(argThatRecord(WorkflowTaskAction.UNCLAIM.name(), "释放任务"));
    }

    @Test
    void complete_shouldRejectWhenActionDisabledByNodeConfig() {
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(false));
        var command = new CompleteWorkflowTaskCommand();
        command.setTaskId("task-1");

        assertThatThrownBy(() -> service.complete(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前节点未启用该审批动作");
    }

    @Test
    void complete_shouldRejectWhenNodeRequiresCommentAndCommentIsBlank() {
        when(repositoryService.getBpmnModel("pd-1")).thenReturn(bpmnModel(true, true));
        CompleteWorkflowTaskCommand command = new CompleteWorkflowTaskCommand();
        command.setTaskId("task-1");
        command.setComment("   ");

        assertThatThrownBy(() -> service.complete(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请填写审批意见");
        verify(taskService, never()).complete(eq("task-1"), any());
    }

    @Test
    void detail_shouldReturnBusinessErrorWhenProcessDefinitionMissing() {
        HistoricProcessInstance historicInstance = mock(HistoricProcessInstance.class);
        when(historicInstance.getId()).thenReturn("proc-1");
        when(historicInstance.getProcessDefinitionId()).thenReturn("pd-1");
        when(historicInstance.getProcessDefinitionName()).thenReturn("测试流程");
        when(historicInstance.getProcessDefinitionKey()).thenReturn("test_process");
        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("proc-1")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historicInstance);
        HistoricVariableInstanceQuery variableQuery = mock(HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(variableQuery);
        when(variableQuery.processInstanceId("proc-1")).thenReturn(variableQuery);
        when(variableQuery.variableName("mangoInitiatorName")).thenReturn(variableQuery);
        when(variableQuery.singleResult()).thenReturn(null);
        when(repositoryService.getBpmnModel("pd-1"))
                .thenThrow(new FlowableObjectNotFoundException("no deployed process definition found"));

        assertThatThrownBy(() -> service.detail("task-1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前任务引用的流程定义不存在");
    }

    @Test
    void detail_shouldFallbackToDefinitionFormWhenFormInstanceMissing() {
        HistoricProcessInstance historicInstance = mock(HistoricProcessInstance.class);
        when(historicInstance.getId()).thenReturn("proc-1");
        when(historicInstance.getBusinessKey()).thenReturn("BIZ-1");
        when(historicInstance.getProcessDefinitionId()).thenReturn("pd-1");
        when(historicInstance.getProcessDefinitionName()).thenReturn("测试流程");
        when(historicInstance.getProcessDefinitionKey()).thenReturn("test_process");
        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("proc-1")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historicInstance);
        HistoricVariableInstanceQuery variableQuery = mock(HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(variableQuery);
        when(variableQuery.processInstanceId("proc-1")).thenReturn(variableQuery);
        when(variableQuery.variableName("mangoInitiatorName")).thenReturn(variableQuery);
        when(variableQuery.singleResult()).thenReturn(null);
        when(runtimeService.getVariables("proc-1")).thenReturn(Map.of("mangoDefinitionId", "1001", "reason", "请假"));
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setFormCode("form_leave");
        definition.setFormJson("[{\"type\":\"textarea\",\"field\":\"reason\",\"title\":\"请假原因\"}]");
        when(definitionMapper.selectOne(any())).thenReturn(definition);

        var detail = service.detail("task-1").getData();

        assertThat(detail.getFormCode()).isEqualTo("form_leave");
        assertThat(detail.getFormJson()).contains("请假原因");
        assertThat(detail.getVariables()).containsEntry("reason", "请假");
    }

    @Test
    void detail_shouldDefaultDynamicFormFieldsToReadonlyWhenNodePermissionMissing() {
        HistoricProcessInstance historicInstance = mock(HistoricProcessInstance.class);
        when(historicInstance.getId()).thenReturn("proc-1");
        when(historicInstance.getBusinessKey()).thenReturn("BIZ-1");
        when(historicInstance.getProcessDefinitionId()).thenReturn("pd-1");
        when(historicInstance.getProcessDefinitionName()).thenReturn("测试流程");
        when(historicInstance.getProcessDefinitionKey()).thenReturn("test_process");
        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("proc-1")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historicInstance);
        HistoricVariableInstanceQuery variableQuery = mock(HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(variableQuery);
        when(variableQuery.processInstanceId("proc-1")).thenReturn(variableQuery);
        when(variableQuery.variableName("mangoInitiatorName")).thenReturn(variableQuery);
        when(variableQuery.singleResult()).thenReturn(null);
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setFormCode("claim_form");
        definition.setFormJson("""
                {"rules":[
                  {"type":"input","field":"title","title":"标题"},
                  {"type":"textarea","field":"reason","title":"说明"}
                ]}
                """);
        when(definitionMapper.selectOne(any())).thenReturn(definition);

        var detail = service.detail("task-1").getData();

        assertThat(detail.getFormPermissions())
                .containsEntry("title", "READONLY")
                .containsEntry("reason", "READONLY");
        assertThat(detail.getRenderConfig().getFormPermissions())
                .containsEntry("title", "READONLY")
                .containsEntry("reason", "READONLY");
    }

    @Test
    void myTaskSummary_shouldAggregateCurrentUserTaskStatus() {
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

        var summary = service.myTaskSummary().getData();

        assertThat(summary.getPending()).isEqualTo(3L);
        assertThat(summary.getProcessing()).isEqualTo(5L);
        assertThat(summary.getCompleted()).isEqualTo(7L);
        assertThat(summary.getOverdue()).isEqualTo(2L);
        assertThat(summary.getTotal()).isEqualTo(17L);
    }

    private org.mockito.ArgumentMatcher<io.mango.workflow.core.entity.WorkflowTaskRecord> recordMatcher(String action, String comment) {
        return record -> action.equals(record.getAction()) && comment.equals(record.getComment());
    }

    private io.mango.workflow.core.entity.WorkflowTaskRecord argThatRecord(String action, String comment) {
        return org.mockito.ArgumentMatchers.argThat(recordMatcher(action, comment));
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

    private TaskQuery taskQuery(Task result) {
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId("task-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(result);
        when(query.taskCandidateOrAssigned(any())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        return query;
    }

    private TaskQuery canOperateQuery(boolean allowed) {
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId("task-1")).thenReturn(query);
        when(query.taskCandidateOrAssigned("anonymous")).thenReturn(query);
        when(query.count()).thenReturn(allowed ? 1L : 0L);
        return query;
    }

    private TaskQuery countTaskQuery(long count) {
        TaskQuery query = mock(TaskQuery.class);
        when(query.or()).thenReturn(query);
        when(query.endOr()).thenReturn(query);
        when(query.taskCandidateUser(any())).thenReturn(query);
        when(query.taskCandidateOrAssigned(any())).thenReturn(query);
        when(query.taskCandidateGroupIn(any())).thenReturn(query);
        when(query.taskUnassigned()).thenReturn(query);
        when(query.taskAssignee(any())).thenReturn(query);
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
                {"actions":{"complete":{"enabled":%s,"requireComment":%s},"save":{"enabled":true},"transfer":{"enabled":true},"addSign":{"enabled":true},"reject":{"enabled":true}}}
                """.formatted(completeEnabled, completeRequireComment));
        userTask.getExtensionElements().put("mangoApprovalConfig", List.of(element));
        Process process = new Process();
        process.addFlowElement(userTask);
        model.addProcess(process);
        return model;
    }
}
