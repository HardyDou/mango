package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.query.WorkflowBusinessApplyProgressBatchQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 业务工作流申请中心接口。
 */
@RestController
@RequestMapping("/workflow/business-applies")
@RequiredArgsConstructor
@Tag(name = "协同办公业务申请中心", description = "业务申请与流程实例关系、进度、历史查询接口")
public class WorkflowBusinessApplyController {

    private final IWorkflowBusinessApplyService workflowBusinessApplyService;

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:create")
    @Operation(summary = "创建业务工作流申请")
    public R<WorkflowBusinessApplyVO> create(@Valid @RequestBody CreateWorkflowBusinessApplyCommand command) {
        return workflowBusinessApplyService.create(command);
    }

    @PostMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "分页查询业务工作流申请")
    public R<PageResult<WorkflowBusinessApplyVO>> page(@RequestBody(required = false) WorkflowBusinessApplyPageQuery query) {
        return workflowBusinessApplyService.page(query);
    }

    @GetMapping("/{applyId}")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:detail")
    @Operation(summary = "查询业务工作流申请详情")
    public R<WorkflowBusinessApplyVO> detail(@PathVariable Long applyId) {
        return workflowBusinessApplyService.detail(applyId);
    }

    @GetMapping("/history")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:detail")
    @Operation(summary = "按业务主键查询申请历史")
    public R<PageResult<WorkflowBusinessApplyVO>> history(@RequestParam String businessType,
                                                         @RequestParam String businessKey,
                                                         @ParameterObject WorkflowBusinessApplyPageQuery query) {
        return workflowBusinessApplyService.history(businessType, businessKey, query);
    }

    @GetMapping("/progress/latest")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:detail")
    @Operation(summary = "查询业务最新申请进度")
    public R<WorkflowBusinessApplyProgressVO> latestProgress(@RequestParam String businessType,
                                                            @RequestParam String businessKey) {
        return workflowBusinessApplyService.latestProgress(businessType, businessKey);
    }

    @PostMapping("/progress/latest-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "批量查询业务最新申请进度")
    public R<Map<String, WorkflowBusinessApplyProgressVO>> latestProgressBatch(
            @Valid @RequestBody WorkflowBusinessApplyProgressBatchQuery query) {
        return R.ok(workflowBusinessApplyService.latestProgress(query.getBusinessType(), query.getBusinessKeys()));
    }

    @GetMapping("/progress/by-process-instance")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:detail")
    @Operation(summary = "按流程实例查询业务申请")
    public R<WorkflowBusinessApplyVO> byProcessInstance(@RequestParam String processInstanceId) {
        return workflowBusinessApplyService.byProcessInstance(processInstanceId);
    }
}
