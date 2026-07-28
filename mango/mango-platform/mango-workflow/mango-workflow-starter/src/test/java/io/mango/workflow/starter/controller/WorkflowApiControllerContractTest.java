package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.WorkflowTaskRuntimeApi;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowProcessService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowApiControllerContractTest {

    private final IWorkflowBusinessApplyService businessApplyService = mock(IWorkflowBusinessApplyService.class);
    private final IWorkflowProcessService processService = mock(IWorkflowProcessService.class);
    private final IWorkflowTaskRuntimeService runtimeService = mock(IWorkflowTaskRuntimeService.class);
    private final WorkflowBusinessApplyController businessApplyController =
            new WorkflowBusinessApplyController(businessApplyService);
    private final WorkflowProcessController processController = new WorkflowProcessController(processService);
    private final WorkflowBusinessProcessController businessProcessController =
            new WorkflowBusinessProcessController(processService);
    private final WorkflowTaskController taskController = new WorkflowTaskController(runtimeService);

    @Test
    void controllers_carryWorkflowApiContracts() {
        assertThat(businessApplyController).isInstanceOf(WorkflowBusinessApplyApi.class);
        assertThat(processController).isInstanceOf(WorkflowProcessApi.class);
        assertThat(businessProcessController).isInstanceOf(WorkflowBusinessProcessApi.class);
        assertThat(taskController).isInstanceOf(WorkflowTaskRuntimeApi.class);
    }

    @Test
    void businessDetailEndpointsRequireLoginWithoutResourcePermission() throws NoSuchMethodException {
        assertLoginAccess(WorkflowProcessController.class, "detail");
        assertLoginAccess(WorkflowTaskController.class, "detail");
        assertLoginAccess(WorkflowTaskController.class, "processDetail");
    }

    @Test
    void taskQueryMethods_delegateToRuntimeService() {
        WorkflowTaskPageQuery query = new WorkflowTaskPageQuery();
        PageResult<WorkflowTaskVO> page = PageResult.of(List.of(), 0, 1, 10);
        WorkflowTaskSummaryVO taskSummary = new WorkflowTaskSummaryVO();
        WorkflowMyTaskSummaryVO mySummary = new WorkflowMyTaskSummaryVO();
        WorkflowTaskDetailVO detail = new WorkflowTaskDetailVO();
        WorkflowProcessDetailVO processDetail = new WorkflowProcessDetailVO();
        when(runtimeService.todo(query)).thenReturn(page);
        when(runtimeService.done(query)).thenReturn(page);
        when(runtimeService.copied(query)).thenReturn(page);
        when(runtimeService.summary()).thenReturn(taskSummary);
        when(runtimeService.myTaskSummary()).thenReturn(mySummary);
        when(runtimeService.detail("task-1")).thenReturn(detail);
        when(runtimeService.processDetail("process-1")).thenReturn(processDetail);

        assertThat(taskController.todo(query).getData()).isSameAs(page);
        assertThat(taskController.done(query).getData()).isSameAs(page);
        assertThat(taskController.copied(query).getData()).isSameAs(page);
        assertThat(taskController.summary().getData()).isSameAs(taskSummary);
        assertThat(taskController.myTaskSummary().getData()).isSameAs(mySummary);
        assertThat(taskController.detail("task-1").getData()).isSameAs(detail);
        assertThat(taskController.processDetail("process-1").getData()).isSameAs(processDetail);

        verify(runtimeService).todo(same(query));
        verify(runtimeService).done(same(query));
        verify(runtimeService).copied(same(query));
        verify(runtimeService).summary();
        verify(runtimeService).myTaskSummary();
        verify(runtimeService).detail("task-1");
        verify(runtimeService).processDetail("process-1");
    }

    @Test
    void taskActionMethods_delegateToRuntimeService() {
        CompleteWorkflowTaskCommand complete = new CompleteWorkflowTaskCommand();
        ClaimWorkflowTaskCommand claim = new ClaimWorkflowTaskCommand();
        ReadWorkflowCopiedTaskCommand readCopied = new ReadWorkflowCopiedTaskCommand();
        WorkflowTaskCompleteResultVO completeResult = new WorkflowTaskCompleteResultVO();
        when(runtimeService.complete(complete)).thenReturn(Boolean.TRUE);
        when(runtimeService.completeWithResult(complete)).thenReturn(completeResult);
        when(runtimeService.reject(null)).thenReturn(Boolean.TRUE);
        when(runtimeService.returnTask(null)).thenReturn(completeResult);
        when(runtimeService.saveDraft(null)).thenReturn(Boolean.TRUE);
        when(runtimeService.transfer(null)).thenReturn(Boolean.TRUE);
        when(runtimeService.addSign(null)).thenReturn(Boolean.TRUE);
        when(runtimeService.claim(claim)).thenReturn(Boolean.TRUE);
        when(runtimeService.unclaim(claim)).thenReturn(Boolean.TRUE);
        when(runtimeService.readCopied(readCopied)).thenReturn(Boolean.TRUE);

        assertThat(taskController.complete(complete).getData()).isTrue();
        assertThat(taskController.completeWithResult(complete).getData()).isSameAs(completeResult);
        assertThat(taskController.reject(null).getData()).isTrue();
        assertThat(taskController.returnTask(null).getData()).isSameAs(completeResult);
        assertThat(taskController.saveDraft(null).getData()).isTrue();
        assertThat(taskController.transfer(null).getData()).isTrue();
        assertThat(taskController.addSign(null).getData()).isTrue();
        assertThat(taskController.claim(claim).getData()).isTrue();
        assertThat(taskController.unclaim(claim).getData()).isTrue();
        assertThat(taskController.readCopied(readCopied).getData()).isTrue();

        verify(runtimeService).complete(complete);
        verify(runtimeService).completeWithResult(complete);
        verify(runtimeService).reject(null);
        verify(runtimeService).returnTask(null);
        verify(runtimeService).saveDraft(null);
        verify(runtimeService).transfer(null);
        verify(runtimeService).addSign(null);
        verify(runtimeService).claim(claim);
        verify(runtimeService).unclaim(claim);
        verify(runtimeService).readCopied(readCopied);
    }

    private void assertLoginAccess(Class<?> controllerType, String methodName) throws NoSuchMethodException {
        ApiAccess apiAccess = controllerType.getDeclaredMethod(methodName, String.class).getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(apiAccess.permission()).isBlank();
    }
}
