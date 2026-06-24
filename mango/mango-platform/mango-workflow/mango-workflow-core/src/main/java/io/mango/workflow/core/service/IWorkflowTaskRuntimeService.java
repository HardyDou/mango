package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;

/**
 * 工作流任务运行时服务。
 */
public interface IWorkflowTaskRuntimeService {

    R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> copied(WorkflowTaskPageQuery query);

    R<WorkflowTaskSummaryVO> summary();

    R<WorkflowTaskDetailVO> detail(String taskId);

    R<Boolean> complete(CompleteWorkflowTaskCommand command);

    R<Boolean> reject(RejectWorkflowTaskCommand command);

    R<Boolean> saveDraft(SaveWorkflowTaskDraftCommand command);

    R<Boolean> transfer(TransferWorkflowTaskCommand command);

    R<Boolean> addSign(AddSignWorkflowTaskCommand command);

    R<Boolean> claim(ClaimWorkflowTaskCommand command);

    R<Boolean> unclaim(ClaimWorkflowTaskCommand command);

    R<Boolean> readCopied(ReadWorkflowCopiedTaskCommand command);

    R<WorkflowProcessDetailVO> processDetail(String processInstanceId);

    void advanceRuntimeTasks(String processInstanceId);
}
