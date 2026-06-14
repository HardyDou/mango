package io.mango.workflow.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowProcessServiceImplTest {

    @Mock
    private WorkflowDefinitionMapper definitionMapper;
    @Mock
    private WorkflowFormInstanceMapper formInstanceMapper;
    @Mock
    private WorkflowTaskRecordMapper taskRecordMapper;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private IWorkflowTaskRuntimeService workflowTaskRuntimeService;
    @Mock
    private IWorkflowBusinessApplyService workflowBusinessApplyService;
    @Mock
    private io.mango.workflow.core.event.WorkflowEventPublisher workflowEventPublisher;

    private WorkflowProcessServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkflowProcessServiceImpl(
                definitionMapper,
                formInstanceMapper,
                taskRecordMapper,
                runtimeService,
                taskService,
                historyService,
                new ObjectMapper(),
                workflowTaskRuntimeService,
                workflowBusinessApplyService,
                workflowEventPublisher);
    }

    @Test
    void start_shouldInferCustomPageRenderModeFromDefinitionFormJson() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1001L);
        definition.setDefinitionKey("contract_seal_approval");
        definition.setDefinitionName("合同用印审批");
        definition.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        definition.setProcessDefinitionId("contract_seal_approval:1:1001");
        definition.setFormCode("form_contract_seal_approval");
        definition.setFormJson("""
                {"mode":"CUSTOM","customConfig":{"approvePageKey":"workflow.contractSeal.approve"}}
                """);
        when(definitionMapper.selectById(1001L)).thenReturn(definition);

        WorkflowBusinessApplyVO apply = new WorkflowBusinessApplyVO();
        apply.setId(2001L);
        when(workflowBusinessApplyService.create(any(CreateWorkflowBusinessApplyCommand.class))).thenReturn(R.ok(apply));

        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessInstanceId()).thenReturn("proc-1");
        when(instance.getBusinessKey()).thenReturn("BIZ-1");
        when(runtimeService.startProcessInstanceById(any(), any(), any(Map.class))).thenReturn(instance);
        ProcessInstanceQuery processInstanceQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("proc-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(instance);

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("proc-1")).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.listPage(0, 1)).thenReturn(java.util.List.of());

        StartWorkflowProcessCommand command = new StartWorkflowProcessCommand();
        command.setDefinitionId(1001L);
        command.setBusinessType("contract_seal");
        command.setBusinessKey("BIZ-1");

        service.start(command);

        ArgumentCaptor<CreateWorkflowBusinessApplyCommand> captor =
                ArgumentCaptor.forClass(CreateWorkflowBusinessApplyCommand.class);
        verify(workflowBusinessApplyService).create(captor.capture());
        assertThat(captor.getValue().getRenderMode()).isEqualTo(WorkflowApplyRenderMode.CUSTOM_PAGE);
    }
}
