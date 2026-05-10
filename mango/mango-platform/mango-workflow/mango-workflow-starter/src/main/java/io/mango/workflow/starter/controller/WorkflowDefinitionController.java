package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
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
 * 工作流定义管理接口。
 */
@RestController
@RequestMapping("/workflow/definitions")
@RequiredArgsConstructor
@Tag(name = "工作流定义", description = "流程定义列表、详情、新增、修改、删除、启停与发布接口")
public class WorkflowDefinitionController {

    private final IWorkflowDefinitionService workflowDefinitionService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:list")
    @Operation(summary = "分页查询流程定义", description = "权限接口。分页查询流程定义配置")
    public R<PageResult<WorkflowDefinitionVO>> page(@ParameterObject WorkflowDefinitionPageQuery query) {
        return workflowDefinitionService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "获取流程定义详情", description = "权限接口。按流程定义ID查询详情，包含设计器JSON和最近一次发布BPMN XML")
    public R<WorkflowDefinitionVO> get(
            @Parameter(description = "流程定义ID", required = true)
            @RequestParam Long id) {
        return workflowDefinitionService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:add")
    @Operation(summary = "新增流程定义", description = "权限接口。创建流程定义草稿")
    public R<Long> create(
            @Parameter(description = "保存流程定义命令", required = true)
            @Valid @RequestBody SaveWorkflowDefinitionCommand command) {
        return workflowDefinitionService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "修改流程定义", description = "权限接口。更新流程定义配置和设计器JSON草稿")
    public R<Boolean> update(
            @Parameter(description = "保存流程定义命令", required = true)
            @Valid @RequestBody SaveWorkflowDefinitionCommand command) {
        return workflowDefinitionService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:delete")
    @Operation(summary = "删除流程定义", description = "权限接口。删除未发布或已停用的流程定义配置")
    public R<Boolean> delete(
            @Parameter(description = "流程定义ID", required = true)
            @RequestParam Long id) {
        return workflowDefinitionService.delete(id);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:status")
    @Operation(summary = "修改流程定义状态", description = "权限接口。修改流程定义状态，支持草稿、已发布和停用")
    public R<Boolean> updateStatus(
            @Parameter(description = "修改流程定义状态命令", required = true)
            @Valid @RequestBody UpdateWorkflowDefinitionStatusCommand command) {
        return workflowDefinitionService.updateStatus(command);
    }

    @PostMapping("/deploy")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:deploy")
    @Operation(summary = "发布流程定义", description = "权限接口。将当前设计器JSON转换为 BPMN 并部署到 Flowable 引擎，同时记录发布版本")
    public R<WorkflowDeployVO> deploy(
            @Parameter(description = "流程定义ID", required = true)
            @RequestParam Long id) {
        return workflowDefinitionService.deploy(id);
    }

    @GetMapping("/versions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "查询流程发布版本", description = "权限接口。查询指定流程定义的历史发布版本")
    public R<List<WorkflowDefinitionVersionVO>> versions(@ParameterObject WorkflowDefinitionVersionQuery query) {
        return workflowDefinitionService.versions(query);
    }

    @GetMapping("/version-detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "获取流程发布版本详情", description = "权限接口。按发布版本ID查询设计器JSON和BPMN XML快照")
    public R<WorkflowDefinitionVersionVO> versionDetail(
            @Parameter(description = "发布版本ID", required = true)
            @RequestParam Long id) {
        return workflowDefinitionService.versionDetail(id);
    }

    @GetMapping("/node-catalog")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "查询工作流设计器节点目录", description = "权限接口。查询通用节点和保函业务节点模板")
    public R<List<WorkflowNodeCatalogVO>> nodeCatalog() {
        return workflowDefinitionService.nodeCatalog();
    }
}
