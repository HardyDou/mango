package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
import io.mango.workflow.core.service.IWorkflowProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;


/**
 * 审批中心流程实例接口。
 */
@RestController
@RequestMapping("/workflow/processes")
@RequiredArgsConstructor
@Validated
@Tag(name = "审批中心流程实例", description = "流程发起与我发起的流程查询接口")
public class WorkflowProcessController implements WorkflowProcessApi {

    private final IWorkflowProcessService workflowProcessService;

    @PostMapping("/start")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:start")
    @Operation(summary = "发起流程", description = "按已发布流程定义创建流程实例")
    @Override
    public R<WorkflowProcessInstanceVO> start(@Valid @RequestBody StartWorkflowProcessCommand command) {
        return R.ok(workflowProcessService.start(command));
    }

    @PostMapping("/start-business")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:start")
    @Operation(summary = "创建业务申请并发起流程", description = "一次性创建业务申请、发起流程并返回当前任务快照")
    @Override
    public R<WorkflowStartResultVO> startBusinessWorkflow(@Valid @RequestBody StartBusinessWorkflowCommand command) {
        return R.ok(workflowProcessService.startBusinessWorkflow(command));
    }

    @GetMapping("/initiated")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的发起流程", description = "分页查询当前用户发起的流程实例")
    @Override
    public R<PageResult<WorkflowProcessInstanceVO>> initiated(
            @Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowProcessService.initiated(query));
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "查询流程实例详情", description = "按流程实例ID查询流程、表单和审批记录")
    @Override
    public R<WorkflowProcessDetailVO> detail(
            @Parameter(description = "流程实例ID", required = true)
            @RequestParam("processInstanceId") String processInstanceId) {
        return R.ok(workflowProcessService.detail(processInstanceId));
    }

    @GetMapping("/history")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "按业务主键查询流程历史", description = "按业务主键分页查询历次流程实例")
    @Override
    public R<PageResult<WorkflowProcessInstanceVO>> history(
            @Parameter(description = "业务主键", required = true)
            @RequestParam("businessKey") String businessKey,
            @Valid @ParameterObject WorkflowTaskPageQuery query) {
        return R.ok(workflowProcessService.historyByBusinessKey(businessKey, query));
    }
}
