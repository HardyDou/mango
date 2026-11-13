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
import io.mango.workflow.api.request.WorkflowTaskTraceRequest;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 工作流任务运行时 API。
 */
public interface WorkflowTaskRuntimeApi {

    R<PageResult<WorkflowTaskVO>> todo(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> initiated(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> done(@Valid WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> copied(@Valid WorkflowTaskPageQuery query);

    /**
     * 按业务类型查询任务，业务主键可选。
     *
     * <p>返回该业务类型下所有申请的全部任务，包含在途、已办理和已终止。数据范围由查询对象决定：
     * 默认 {@code SELF} 仅返回当前用户相关任务；{@code ALL} 需要 {@code workflow:task:all} 权限码；
     * {@code ORG} 当前版本不支持。租户隔离始终生效。
     *
     * @param request 任务查询条件；{@code businessKeys} 不传或为空表示不限业务主键
     * @return 任务分页结果；不传分页参数时返回全部结果，{@code total} 为过滤后总数
     */
    R<PageResult<WorkflowTaskVO>> pageByBusinessKey(@Valid WorkflowTaskBusinessPageRequest request);

    /**
     * 按业务键读取审批流转记录。
     *
     * <p>返回该业务键下全部流程实例的任务级流转记录，包含发起（{@code START}）、认领（{@code CLAIM}）、
     * 通过（{@code COMPLETE}）、退回（{@code RETURN}）、驳回（{@code REJECT}）等动作，按处理时间升序、
     * 同一时间按记录 ID 升序排列。重新申请产生的多个流程实例全部返回，调用方按 {@code processInstanceId} 分组。
     * <p>本方法不做数据范围与申请级权限校验：不读取 {@code workflow_business_apply} 的访问权限，也不回落到
     * “仅申请人可读”。租户隔离始终生效。网关节点与撤回动作不在本方法返回范围内。
     * <p>不返回处理变量（{@code variables} 恒为 null）；记录数超过 {@code mango.workflow.task-query.unpaged-max-rows}
     * 时以 {@code TASK_QUERY_INVALID} 失败，不做静默截断。
     *
     * @param request 业务类型、业务主键与可选过滤条件；业务类型和业务主键必填
     * @return 流转记录列表；无命中、跨租户或流程未登记业务申请时返回空集合
     */
    R<List<WorkflowTaskRecordVO>> traceByBusinessKey(@Valid WorkflowTaskTraceRequest request);

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
