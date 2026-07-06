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

import java.util.List;

/**
 * 审批中心任务接口。
 */
@RestController
@RequestMapping("/workflow/tasks")
@RequiredArgsConstructor
@Tag(name = "审批中心任务", description = "我的待办、我的发起、我的已办、抄送给我任务查询接口")
public class WorkflowTaskController implements WorkflowTaskRuntimeApi {

    private final IWorkflowTaskRuntimeService workflowTaskRuntimeService;

    @GetMapping("/todo")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的待办")
    @Override
    public R<PageResult<WorkflowTaskVO>> todo(@ParameterObject WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.todo(query);
    }

    @GetMapping("/todo/summary")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的待办统计")
    @Override
    public R<WorkflowTaskSummaryVO> summary() {
        return workflowTaskRuntimeService.summary();
    }

    @GetMapping("/my/summary")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的任务统计", description = "统计当前登录人的待完成、进行中、已完成和已逾期任务数量")
    @Override
    public R<WorkflowMyTaskSummaryVO> myTaskSummary() {
        return workflowTaskRuntimeService.myTaskSummary();
    }

    @GetMapping("/initiated")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的发起")
    public R<PageResult<WorkflowTaskVO>> initiated(@ParameterObject WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        return R.ok(PageResult.of(List.of(), 0, resolved.getPage(), resolved.getSize()));
    }

    @GetMapping("/done")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的已办")
    @Override
    public R<PageResult<WorkflowTaskVO>> done(@ParameterObject WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.done(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:detail")
    @Operation(summary = "查询任务详情")
    @Override
    public R<WorkflowTaskDetailVO> detail(@RequestParam String taskId) {
        return workflowTaskRuntimeService.detail(taskId);
    }

    @PostMapping("/complete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:complete")
    @Operation(summary = "审批通过")
    @Override
    public R<Boolean> complete(@Valid @RequestBody CompleteWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.complete(command);
    }

    @PostMapping("/complete-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:complete")
    @Operation(summary = "审批通过并返回推进后结果", description = "返回流程推进完成后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskCompleteResultVO> completeWithResult(@Valid @RequestBody CompleteWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.completeWithResult(command);
    }

    @PostMapping("/reject")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:reject")
    @Operation(summary = "审批驳回")
    @Override
    public R<Boolean> reject(@Valid @RequestBody RejectWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.reject(command);
    }

    @PostMapping("/reject-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:reject")
    @Operation(summary = "审批驳回并返回结果", description = "返回驳回后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> rejectWithResult(@Valid @RequestBody RejectWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.rejectWithResult(command);
    }

    @PostMapping("/return")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:return")
    @Operation(summary = "审批退回", description = "退回到最近一个已完成的不同用户任务节点或指定历史节点，并返回退回后的当前任务快照")
    @Override
    public R<WorkflowTaskCompleteResultVO> returnTask(@Valid @RequestBody ReturnWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.returnTask(command);
    }

    @PostMapping("/save")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:save")
    @Operation(summary = "暂存审批任务")
    @Override
    public R<Boolean> saveDraft(@Valid @RequestBody SaveWorkflowTaskDraftCommand command) {
        return workflowTaskRuntimeService.saveDraft(command);
    }

    @PostMapping("/save-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:save")
    @Operation(summary = "暂存审批任务并返回结果", description = "返回暂存后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> saveDraftWithResult(@Valid @RequestBody SaveWorkflowTaskDraftCommand command) {
        return workflowTaskRuntimeService.saveDraftWithResult(command);
    }

    @PostMapping("/transfer")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:transfer")
    @Operation(summary = "转办审批任务")
    @Override
    public R<Boolean> transfer(@Valid @RequestBody TransferWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.transfer(command);
    }

    @PostMapping("/add-sign")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:add-sign")
    @Operation(summary = "加签审批任务")
    @Override
    public R<Boolean> addSign(@Valid @RequestBody AddSignWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.addSign(command);
    }

    @PostMapping("/claim")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:claim")
    @Operation(summary = "认领候选任务")
    @Override
    public R<Boolean> claim(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.claim(command);
    }

    @PostMapping("/claim-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:claim")
    @Operation(summary = "认领候选任务并返回结果", description = "返回认领后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> claimWithResult(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.claimWithResult(command);
    }

    @PostMapping("/unclaim")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:unclaim")
    @Operation(summary = "释放候选任务")
    @Override
    public R<Boolean> unclaim(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.unclaim(command);
    }

    @PostMapping("/unclaim-result")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:unclaim")
    @Operation(summary = "释放候选任务并返回结果", description = "返回释放后的业务申请状态和当前任务快照")
    @Override
    public R<WorkflowTaskActionResultVO> unclaimWithResult(@Valid @RequestBody ClaimWorkflowTaskCommand command) {
        return workflowTaskRuntimeService.unclaimWithResult(command);
    }

    @GetMapping("/copied")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询抄送给我")
    @Override
    public R<PageResult<WorkflowTaskVO>> copied(@ParameterObject WorkflowTaskPageQuery query) {
        return workflowTaskRuntimeService.copied(query);
    }

    @PostMapping("/copied/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:read-copied")
    @Operation(summary = "标记抄送已阅")
    @Override
    public R<Boolean> readCopied(@Valid @RequestBody ReadWorkflowCopiedTaskCommand command) {
        return workflowTaskRuntimeService.readCopied(command);
    }

    @GetMapping("/process-detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "按任务接口查询流程实例详情")
    @Override
    public R<WorkflowProcessDetailVO> processDetail(@RequestParam String processInstanceId) {
        return workflowTaskRuntimeService.processDetail(processInstanceId);
    }

    private WorkflowTaskPageQuery resolve(WorkflowTaskPageQuery query) {
        return query == null ? new WorkflowTaskPageQuery() : query;
    }
}
