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

    /**
     * 把当前任务退回历史用户任务节点。
     * <p>
     * 退回使用流程引擎的变更活动状态能力，不要求源节点与目标节点之间存在回退连线，也不要求在目标节点配置退回动作；
     * 只有当前任务的 {@code returnTask} 动作必须启用。{@code targetTaskDefinitionKey} 指向当前流程实例中已完成过的
     * 用户任务节点；省略时退回最近一个已完成的不同用户任务节点。传入网关或汇聚节点时按流程定义解析到上游最近的已完成用户任务，
     * 多个候选或没有候选时失败。
     * <p>
     * 退回成功后返回 {@code actionResult=RETURN} 和刷新后的当前任务快照。目标节点没有产生可办理任务、或流程仍停在原节点时，
     * 调用以 {@code TASK_INVALID} 失败并回滚，不会返回成功。
     */
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
