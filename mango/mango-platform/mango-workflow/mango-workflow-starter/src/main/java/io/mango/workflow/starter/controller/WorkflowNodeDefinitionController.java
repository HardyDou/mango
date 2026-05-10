package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowNodeDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowNodeDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowNodeDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowNodeDefinitionVO;
import io.mango.workflow.core.service.IWorkflowNodeDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流节点定义管理接口。
 */
@RestController
@RequestMapping("/workflow/node-definitions")
@RequiredArgsConstructor
@Tag(name = "工作流节点定义", description = "流程设计器节点模板、节点能力和节点显示配置管理接口")
public class WorkflowNodeDefinitionController {

    private final IWorkflowNodeDefinitionService workflowNodeDefinitionService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "分页查询流程节点定义", description = "权限接口。分页查询流程设计器可用节点模板")
    public R<PageResult<WorkflowNodeDefinitionVO>> page(@ParameterObject WorkflowNodeDefinitionPageQuery query) {
        return workflowNodeDefinitionService.page(query);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "查询流程节点定义列表", description = "权限接口。查询流程设计器节点模板列表，可按状态过滤")
    public R<List<WorkflowNodeDefinitionVO>> list(
            @Parameter(description = "状态：0-停用，1-启用")
            @RequestParam(required = false) Integer status) {
        return workflowNodeDefinitionService.list(status);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "获取流程节点定义详情", description = "权限接口。按节点定义ID查询节点模板详情")
    public R<WorkflowNodeDefinitionVO> get(
            @Parameter(description = "节点定义ID", required = true)
            @RequestParam Long id) {
        return workflowNodeDefinitionService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "新增流程节点定义", description = "权限接口。新增流程设计器节点模板")
    public R<Long> create(
            @Parameter(description = "保存流程节点定义命令", required = true)
            @Valid @RequestBody SaveWorkflowNodeDefinitionCommand command) {
        return workflowNodeDefinitionService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "修改流程节点定义", description = "权限接口。修改流程设计器节点模板")
    public R<Boolean> update(
            @Parameter(description = "保存流程节点定义命令", required = true)
            @Valid @RequestBody SaveWorkflowNodeDefinitionCommand command) {
        return workflowNodeDefinitionService.update(command);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "修改流程节点定义状态", description = "权限接口。启用或停用流程设计器节点模板")
    public R<Boolean> updateStatus(
            @Parameter(description = "修改流程节点定义状态命令", required = true)
            @Valid @RequestBody UpdateWorkflowNodeDefinitionStatusCommand command) {
        return workflowNodeDefinitionService.updateStatus(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "删除流程节点定义", description = "权限接口。删除未使用的流程设计器节点模板")
    public R<Boolean> delete(
            @Parameter(description = "节点定义ID", required = true)
            @RequestParam Long id) {
        return workflowNodeDefinitionService.delete(id);
    }
}
