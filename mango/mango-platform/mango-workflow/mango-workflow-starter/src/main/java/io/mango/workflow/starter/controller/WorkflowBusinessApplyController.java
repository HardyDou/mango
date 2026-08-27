package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyPageRequest;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 业务工作流申请中心接口。
 */
@RestController
@RequestMapping("/workflow/business-applies")
@RequiredArgsConstructor
@Validated
@Tag(name = "审批中心业务申请", description = "业务申请与流程实例关系、进度、历史查询接口")
public class WorkflowBusinessApplyController implements WorkflowBusinessApplyApi {

    private final IWorkflowBusinessApplyService workflowBusinessApplyService;

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:create")
    @Operation(summary = "创建业务工作流申请", description = "创建业务申请并保存流程变量与展示快照")
    @Override
    public R<WorkflowBusinessApplyVO> create(@RequestBody CreateWorkflowBusinessApplyCommand command) {
        return R.ok(workflowBusinessApplyService.create(command));
    }

    @PostMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "分页查询业务工作流申请", description = "按业务、状态和申请人条件分页查询申请记录")
    @Override
    public R<PageResult<WorkflowBusinessApplyVO>> page(
            @RequestBody WorkflowBusinessApplyPageRequest request) {
        return R.ok(workflowBusinessApplyService.page(request));
    }

    @GetMapping("/my/summary")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的申请统计", description = "统计当前用户各状态的业务申请数量")
    @Override
    public R<WorkflowBusinessApplySummaryVO> mySummary() {
        return R.ok(workflowBusinessApplyService.mySummary());
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "已登录用户按业务数据权限查询业务申请详情")
    @Operation(summary = "查询业务工作流申请详情", description = "按申请ID查询业务申请、当前任务和展示快照")
    @Override
    public R<WorkflowBusinessApplyVO> detail(
            @Parameter(description = "业务申请ID", required = true)
            @RequestParam("applyId") Long applyId) {
        return R.ok(workflowBusinessApplyService.detail(applyId));
    }

    @GetMapping("/history")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "已登录用户按业务数据权限查询申请历史")
    @Operation(summary = "按业务主键查询申请历史", description = "按业务类型和业务主键分页查询历次申请")
    @Override
    public R<PageResult<WorkflowBusinessApplyVO>> history(
            @ParameterObject WorkflowBusinessApplyPageQuery query) {
        return R.ok(workflowBusinessApplyService.history(query));
    }

    @GetMapping("/progress/latest")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "已登录用户按业务数据权限查询最新进度")
    @Operation(summary = "查询业务最新申请进度", description = "按业务类型和业务主键查询最新一次申请进度")
    @Override
    public R<WorkflowBusinessApplyProgressVO> latestProgress(
            @Parameter(description = "业务类型", required = true)
            @RequestParam("businessType") String businessType,
            @Parameter(description = "业务主键", required = true)
            @RequestParam("businessKey") String businessKey) {
        return R.ok(workflowBusinessApplyService.latestProgress(businessType, businessKey));
    }

    @PostMapping("/progress/latest-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "批量查询业务最新申请进度", description = "按业务主键集合批量查询每项业务的最新申请进度")
    @Override
    public R<WorkflowBusinessApplyProgressBatchVO> latestProgressBatch(
            @RequestBody WorkflowBusinessApplyProgressBatchRequest request) {
        return R.ok(workflowBusinessApplyService.latestProgressBatch(request));
    }

    @PostMapping("/latest-by-business-keys")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "批量查询最新业务申请", description = "按业务类型和业务主键集合查询最新申请详情")
    @Override
    public R<List<WorkflowBusinessApplyVO>> latestByBusinessKeys(
            @RequestBody WorkflowBusinessApplyProgressBatchRequest request) {
        return R.ok(workflowBusinessApplyService
                .latestByBusinessKeys(request.getBusinessType(), request.getBusinessKeys()));
    }

    @GetMapping("/progress/by-process-instance")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "已登录用户按业务数据权限查询流程申请")
    @Operation(summary = "按流程实例查询业务申请", description = "按流程实例ID查询关联的业务申请")
    @Override
    public R<WorkflowBusinessApplyVO> byProcessInstance(
            @Parameter(description = "流程实例ID", required = true)
            @RequestParam("processInstanceId") String processInstanceId) {
        return R.ok(workflowBusinessApplyService.byProcessInstance(processInstanceId));
    }
}
