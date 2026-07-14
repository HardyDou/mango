package io.mango.workflow.api;

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
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 工作流任务运行时 API。
 */
public interface WorkflowTaskRuntimeApi {

    R<PageResult<WorkflowTaskVO>> todo(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> initiated(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> done(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> copied(@Valid WorkflowTaskPageQuery query);

    R<WorkflowTaskSummaryVO> summary();

    R<WorkflowMyTaskSummaryVO> myTaskSummary();

    R<WorkflowTaskDetailVO> detail(@NotBlank String taskId);

    R<Boolean> complete(@Valid CompleteWorkflowTaskCommand command);

    R<WorkflowTaskCompleteResultVO> completeWithResult(@Valid CompleteWorkflowTaskCommand command);

    R<Boolean> reject(@Valid RejectWorkflowTaskCommand command);

    R<WorkflowTaskActionResultVO> rejectWithResult(@Valid RejectWorkflowTaskCommand command);

    R<WorkflowTaskCompleteResultVO> returnTask(@Valid ReturnWorkflowTaskCommand command);

    R<Boolean> saveDraft(@Valid SaveWorkflowTaskDraftCommand command);

    R<WorkflowTaskActionResultVO> saveDraftWithResult(@Valid SaveWorkflowTaskDraftCommand command);

    R<Boolean> transfer(@Valid TransferWorkflowTaskCommand command);

    R<Boolean> addSign(@Valid AddSignWorkflowTaskCommand command);

    R<Boolean> claim(@Valid ClaimWorkflowTaskCommand command);

    R<WorkflowTaskActionResultVO> claimWithResult(@Valid ClaimWorkflowTaskCommand command);

    R<Boolean> unclaim(@Valid ClaimWorkflowTaskCommand command);

    R<WorkflowTaskActionResultVO> unclaimWithResult(@Valid ClaimWorkflowTaskCommand command);

    R<Boolean> readCopied(@Valid ReadWorkflowCopiedTaskCommand command);

    R<WorkflowProcessDetailVO> processDetail(@NotBlank String processInstanceId);
}
