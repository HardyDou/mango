package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;


/**
 * 审批中心任务接口。
 */
@RestController
@RequestMapping("/workflow/tasks")
@RequiredArgsConstructor
@Validated
@Tag(name = "审批中心任务", description = "我的待办、我的发起、我的已办、抄送给我任务查询接口")
public class WorkflowTaskController implements WorkflowTaskRuntimeApi {

    private final IWorkflowTaskRuntimeService workflowTaskRuntimeService;

    @GetMapping("/todo")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的待办", description = "分页查询当前用户待处理或待领取的审批任务")
    @Override
    public R<PageResult<WorkflowTaskVO>> todo(@Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowTaskRuntimeService.todo(query));
    }

    @GetMapping("/todo/summary")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的待办统计", description = "统计当前用户待审批、待领取、未读抄送和逾期任务")
    @Override
    public R<WorkflowTaskSummaryVO> summary() {
        return R.ok(workflowTaskRuntimeService.summary());
    }

    @GetMapping("/my/summary")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的任务统计", description = "统计当前登录人的待完成、进行中、已完成和已逾期任务数量")
    @Override
    public R<WorkflowMyTaskSummaryVO> myTaskSummary() {
        return R.ok(workflowTaskRuntimeService.myTaskSummary());
    }

    @GetMapping("/initiated")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的发起", description = "保留任务维度的我的发起兼容查询")
    @Override
    public R<PageResult<WorkflowTaskVO>> initiated(@Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowTaskRuntimeService.initiated(query));
    }

    @GetMapping("/done")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的已办", description = "分页查询当前用户已经处理完成的审批任务")
    @Override
    public R<PageResult<WorkflowTaskVO>> done(@Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowTaskRuntimeService.done(query));
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:detail")
    @Operation(summary = "查询任务详情", description = "按任务ID查询流程、表单、权限和审批记录")
    @Override
    public R<WorkflowTaskDetailVO> detail(
            @Parameter(description = "任务ID", required = true)
            @RequestParam("taskId") String taskId) {
        return R.ok(workflowTaskRuntimeService.detail(taskId));
    }

    @PostMapping("/complete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:complete")
    @Operation(summary = "审批通过", description = "完成当前审批任务并推进流程")
    @Override
    public R<Boolean> complete(@Valid @RequestBody CompleteWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.complete(command));
    }

    @PostMapping("/complete-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:complete")
    @Operation(summary = "审批通过并返回推进后结果", description = "返回流程推进完成后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskCompleteResultVO> completeWithResult(@Valid @RequestBody CompleteWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.completeWithResult(command));
    }

    @PostMapping("/reject")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:reject")
    @Operation(summary = "审批驳回", description = "驳回当前任务并结束流程实例")
    @Override
    public R<Boolean> reject(@Valid @RequestBody RejectWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.reject(command));
    }

    @PostMapping("/reject-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:reject")
    @Operation(summary = "审批驳回并返回结果", description = "返回驳回后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> rejectWithResult(@Valid @RequestBody RejectWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.rejectWithResult(command));
    }

    @PostMapping("/return")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:return")
    @Operation(summary = "审批退回", description = "退回到最近一个已完成的不同用户任务节点或指定历史节点，并返回退回后的当前任务快照")
    @Override
    public R<WorkflowTaskCompleteResultVO> returnTask(@Valid @RequestBody ReturnWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.returnTask(command));
    }

    @PostMapping("/save")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:save")
    @Operation(summary = "暂存审批任务", description = "保存当前表单变量但不推进流程")
    @Override
    public R<Boolean> saveDraft(@Valid @RequestBody SaveWorkflowTaskDraftCommand command) {
        return R.ok(workflowTaskRuntimeService.saveDraft(command));
    }

    @PostMapping("/save-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:save")
    @Operation(summary = "暂存审批任务并返回结果", description = "返回暂存后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> saveDraftWithResult(@Valid @RequestBody SaveWorkflowTaskDraftCommand command) {
        return R.ok(workflowTaskRuntimeService.saveDraftWithResult(command));
    }

    @PostMapping("/transfer")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:transfer")
    @Operation(summary = "转办审批任务", description = "将当前任务转交给指定用户处理")
    @Override
    public R<Boolean> transfer(@Valid @RequestBody TransferWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.transfer(command));
    }

    @PostMapping("/add-sign")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:add-sign")
    @Operation(summary = "加签审批任务", description = "为当前审批节点增加处理用户")
    @Override
    public R<Boolean> addSign(@Valid @RequestBody AddSignWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.addSign(command));
    }

    @PostMapping("/claim")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:claim")
    @Operation(summary = "认领候选任务", description = "当前用户认领有权限处理的候选任务")
    @Override
    public R<Boolean> claim(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.claim(command));
    }

    @PostMapping("/claim-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:claim")
    @Operation(summary = "认领候选任务并返回结果", description = "返回认领后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> claimWithResult(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.claimWithResult(command));
    }

    @PostMapping("/unclaim")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:unclaim")
    @Operation(summary = "释放候选任务", description = "释放已认领任务并恢复为候选状态")
    @Override
    public R<Boolean> unclaim(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.unclaim(command));
    }

    @PostMapping("/unclaim-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:unclaim")
    @Operation(summary = "释放候选任务并返回结果", description = "返回释放后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> unclaimWithResult(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.unclaimWithResult(command));
    }

    @GetMapping("/copied")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询抄送给我", description = "分页查询抄送给当前用户的流程消息")
    @Override
    public R<PageResult<WorkflowTaskVO>> copied(@Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowTaskRuntimeService.copied(query));
    }

    @PostMapping("/copied/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:read-copied")
    @Operation(summary = "标记抄送已阅", description = "将指定抄送记录标记为已读")
    @Override
    public R<Boolean> readCopied(@Valid @RequestBody ReadWorkflowCopiedTaskCommand command) {
        return R.ok(workflowTaskRuntimeService.readCopied(command));
    }

    @GetMapping("/process-detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "按任务接口查询流程实例详情", description = "按流程实例ID查询流程、表单和审批记录")
    @Override
    public R<WorkflowProcessDetailVO> processDetail(
            @Parameter(description = "流程实例ID", required = true)
            @RequestParam("processInstanceId") String processInstanceId) {
        return R.ok(workflowTaskRuntimeService.processDetail(processInstanceId));
    }
}
