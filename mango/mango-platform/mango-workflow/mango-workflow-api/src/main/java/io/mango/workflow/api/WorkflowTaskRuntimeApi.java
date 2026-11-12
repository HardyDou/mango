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
import io.mango.workflow.api.request.WorkflowTaskBusinessPageRequest;
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

    /**
     * 按业务主键批量查询全部任务。
     *
     * <p>返回该批业务主键下所有申请的全部任务，包含在途、已办理和已终止。数据范围由查询对象决定：
     * 默认 {@code SELF} 仅返回当前用户相关任务；{@code ALL} 需要 {@code workflow:task:all} 权限码；
     * {@code ORG} 当前版本不支持。租户隔离与业务申请可见性始终生效。
     *
     * @param request 业务主键批量查询条件
     * @return 任务分页结果；不传分页参数时返回全部结果，{@code total} 为过滤后总数
     */
    R<PageResult<WorkflowTaskVO>> pageByBusinessKey(@Valid WorkflowTaskBusinessPageRequest request);

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
     * <p>
     * 可选 {@code targetAssigneeUserId} 指定退回后目标任务的办理人。它只要求调用人能办理当前任务，
     * 不要求调用人是目标节点候选人或具备目标节点的 {@code transfer} 动作。目标用户必须是当前租户启用成员；
     * 指定办理人优先于目标节点的原配置和自动派单结果；本次退回必须在目标节点只新建一条任务，否则以 {@code TASK_INVALID} 失败并回滚。
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
