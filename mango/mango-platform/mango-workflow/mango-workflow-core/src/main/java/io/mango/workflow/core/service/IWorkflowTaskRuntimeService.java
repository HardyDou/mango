package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;

/**
 * 工作流任务运行时服务。
 */
public interface IWorkflowTaskRuntimeService {

    PageResult<WorkflowTaskVO> todo(WorkflowTaskPageQuery query);

    PageResult<WorkflowTaskVO> initiated(WorkflowTaskPageQuery query);

    PageResult<WorkflowTaskVO> done(WorkflowTaskPageQuery query);

    PageResult<WorkflowTaskVO> copied(WorkflowTaskPageQuery query);

    WorkflowTaskSummaryVO summary();

    WorkflowMyTaskSummaryVO myTaskSummary();

    WorkflowTaskDetailVO detail(String taskId);

    Boolean complete(CompleteWorkflowTaskCommand command);

    WorkflowTaskCompleteResultVO completeWithResult(CompleteWorkflowTaskCommand command);

    Boolean reject(RejectWorkflowTaskCommand command);

    WorkflowTaskActionResultVO rejectWithResult(RejectWorkflowTaskCommand command);

    WorkflowTaskCompleteResultVO returnTask(ReturnWorkflowTaskCommand command);

    Boolean saveDraft(SaveWorkflowTaskDraftCommand command);

    WorkflowTaskActionResultVO saveDraftWithResult(SaveWorkflowTaskDraftCommand command);

    Boolean transfer(TransferWorkflowTaskCommand command);

    Boolean addSign(AddSignWorkflowTaskCommand command);

    Boolean claim(ClaimWorkflowTaskCommand command);

    WorkflowTaskActionResultVO claimWithResult(ClaimWorkflowTaskCommand command);

    Boolean unclaim(ClaimWorkflowTaskCommand command);

    WorkflowTaskActionResultVO unclaimWithResult(ClaimWorkflowTaskCommand command);

    Boolean readCopied(ReadWorkflowCopiedTaskCommand command);

    WorkflowProcessDetailVO processDetail(String processInstanceId);

    WorkflowTaskAdvanceResult advanceRuntimeTasks(String processInstanceId);
}
