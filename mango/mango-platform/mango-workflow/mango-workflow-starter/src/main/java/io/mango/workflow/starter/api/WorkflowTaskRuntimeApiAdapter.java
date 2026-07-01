package io.mango.workflow.starter.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowTaskRuntimeApi;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 工作流任务运行时 API 本地适配器。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskRuntimeApiAdapter implements WorkflowTaskRuntimeApi {

    private final IWorkflowTaskRuntimeService workflowTaskRuntimeService;

    @Override
    public R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.todo(query);
    }

    @Override
    public R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.done(query);
    }

    @Override
    public R<PageResult<WorkflowTaskVO>> copied(WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.copied(query);
    }

    @Override
    public R<WorkflowTaskSummaryVO> summary() {
        return workflowTaskRuntimeService.summary();
    }

    @Override
    public R<WorkflowMyTaskSummaryVO> myTaskSummary() {
        return workflowTaskRuntimeService.myTaskSummary();
    }

    @Override
    public R<WorkflowTaskDetailVO> detail(String taskId) {
        return workflowTaskRuntimeService.detail(taskId);
    }

    @Override
    public R<Boolean> complete(CompleteWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.complete(command);
    }

    @Override
    public R<WorkflowTaskCompleteResultVO> completeWithResult(CompleteWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.completeWithResult(command);
    }

    @Override
    public R<Boolean> reject(RejectWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.reject(command);
    }

    @Override
    public R<WorkflowTaskCompleteResultVO> returnTask(ReturnWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.returnTask(command);
    }

    @Override
    public R<Boolean> saveDraft(SaveWorkflowTaskDraftCommand command) {
        return workflowTaskRuntimeService.saveDraft(command);
    }

    @Override
    public R<Boolean> transfer(TransferWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.transfer(command);
    }

    @Override
    public R<Boolean> addSign(AddSignWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.addSign(command);
    }

    @Override
    public R<Boolean> claim(ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.claim(command);
    }

    @Override
    public R<Boolean> unclaim(ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.unclaim(command);
    }

    @Override
    public R<Boolean> readCopied(ReadWorkflowCopiedTaskCommand command) {
        return workflowTaskRuntimeService.readCopied(command);
    }

    @Override
    public R<WorkflowProcessDetailVO> processDetail(String processInstanceId) {
        return workflowTaskRuntimeService.processDetail(processInstanceId);
    }
}
