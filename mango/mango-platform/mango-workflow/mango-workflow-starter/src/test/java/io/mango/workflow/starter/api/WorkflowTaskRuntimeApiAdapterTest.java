package io.mango.workflow.starter.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
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
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowTaskRuntimeApiAdapterTest {

    private final IWorkflowTaskRuntimeService runtimeService = mock(IWorkflowTaskRuntimeService.class);
    private final WorkflowTaskRuntimeApiAdapter adapter = new WorkflowTaskRuntimeApiAdapter(runtimeService);

    @Test
    void queryMethods_delegateToRuntimeService() {
        WorkflowTaskPageQuery query = new WorkflowTaskPageQuery();
        R<PageResult<WorkflowTaskVO>> page = R.ok(PageResult.of(List.of(), 0, 1, 10));
        WorkflowTaskSummaryVO taskSummary = new WorkflowTaskSummaryVO();
        WorkflowMyTaskSummaryVO mySummary = new WorkflowMyTaskSummaryVO();
        WorkflowTaskDetailVO detail = new WorkflowTaskDetailVO();
        WorkflowProcessDetailVO processDetail = new WorkflowProcessDetailVO();
        when(runtimeService.todo(query)).thenReturn(page);
        when(runtimeService.done(query)).thenReturn(page);
        when(runtimeService.copied(query)).thenReturn(page);
        when(runtimeService.summary()).thenReturn(R.ok(taskSummary));
        when(runtimeService.myTaskSummary()).thenReturn(R.ok(mySummary));
        when(runtimeService.detail("task-1")).thenReturn(R.ok(detail));
        when(runtimeService.processDetail("process-1")).thenReturn(R.ok(processDetail));

        assertThat(adapter.todo(query)).isSameAs(page);
        assertThat(adapter.done(query)).isSameAs(page);
        assertThat(adapter.copied(query)).isSameAs(page);
        assertThat(adapter.summary().getData()).isSameAs(taskSummary);
        assertThat(adapter.myTaskSummary().getData()).isSameAs(mySummary);
        assertThat(adapter.detail("task-1").getData()).isSameAs(detail);
        assertThat(adapter.processDetail("process-1").getData()).isSameAs(processDetail);

        verify(runtimeService).todo(same(query));
        verify(runtimeService).done(same(query));
        verify(runtimeService).copied(same(query));
        verify(runtimeService).summary();
        verify(runtimeService).myTaskSummary();
        verify(runtimeService).detail("task-1");
        verify(runtimeService).processDetail("process-1");
    }

    @Test
    void actionMethods_delegateToRuntimeService() {
        CompleteWorkflowTaskCommand complete = new CompleteWorkflowTaskCommand();
        ClaimWorkflowTaskCommand claim = new ClaimWorkflowTaskCommand();
        ReadWorkflowCopiedTaskCommand readCopied = new ReadWorkflowCopiedTaskCommand();
        WorkflowTaskCompleteResultVO completeResult = new WorkflowTaskCompleteResultVO();
        when(runtimeService.complete(complete)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.completeWithResult(complete)).thenReturn(R.ok(completeResult));
        when(runtimeService.reject(null)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.returnTask(null)).thenReturn(R.ok(completeResult));
        when(runtimeService.saveDraft(null)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.transfer(null)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.addSign(null)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.claim(claim)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.unclaim(claim)).thenReturn(R.ok(Boolean.TRUE));
        when(runtimeService.readCopied(readCopied)).thenReturn(R.ok(Boolean.TRUE));

        assertThat(adapter.complete(complete).getData()).isTrue();
        assertThat(adapter.completeWithResult(complete).getData()).isSameAs(completeResult);
        assertThat(adapter.reject(null).getData()).isTrue();
        assertThat(adapter.returnTask(null).getData()).isSameAs(completeResult);
        assertThat(adapter.saveDraft(null).getData()).isTrue();
        assertThat(adapter.transfer(null).getData()).isTrue();
        assertThat(adapter.addSign(null).getData()).isTrue();
        assertThat(adapter.claim(claim).getData()).isTrue();
        assertThat(adapter.unclaim(claim).getData()).isTrue();
        assertThat(adapter.readCopied(readCopied).getData()).isTrue();

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
}
