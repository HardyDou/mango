package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowCategoryCommand;
import io.mango.workflow.api.query.WorkflowCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowCategoryVO;
import io.mango.workflow.core.service.IWorkflowCategoryService;
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
 * 工作流分类管理接口。
 */
@RestController
@RequestMapping("/workflow/categories")
@RequiredArgsConstructor
@Tag(name = "工作流分类", description = "流程分类列表、详情、新增、修改与删除接口")
public class WorkflowCategoryController {

    private final IWorkflowCategoryService workflowCategoryService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:list")
    @Operation(summary = "分页查询流程分类", description = "权限接口。分页查询流程分类配置")
    public R<PageResult<WorkflowCategoryVO>> page(@ParameterObject WorkflowCategoryPageQuery query) {
        return workflowCategoryService.page(query);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:list")
    @Operation(summary = "查询流程分类选项", description = "权限接口。查询流程分类轻量列表，用于流程定义筛选和表单选择")
    public R<List<WorkflowCategoryVO>> list(
            @Parameter(description = "状态：0-停用，1-启用")
            @RequestParam(required = false) Integer status) {
        return workflowCategoryService.list(status);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:query")
    @Operation(summary = "获取流程分类详情", description = "权限接口。按流程分类ID查询详情")
    public R<WorkflowCategoryVO> get(
            @Parameter(description = "流程分类ID", required = true)
            @RequestParam Long id) {
        return workflowCategoryService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:add")
    @Operation(summary = "新增流程分类", description = "权限接口。创建流程分类")
    public R<String> create(
            @Parameter(description = "保存流程分类命令", required = true)
            @Valid @RequestBody SaveWorkflowCategoryCommand command) {
        return workflowCategoryService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:edit")
    @Operation(summary = "修改流程分类", description = "权限接口。更新流程分类")
    public R<Boolean> update(
            @Parameter(description = "保存流程分类命令", required = true)
            @Valid @RequestBody SaveWorkflowCategoryCommand command) {
        return workflowCategoryService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:definition:delete")
    @Operation(summary = "删除流程分类", description = "权限接口。删除未被流程定义引用的流程分类")
    public R<Boolean> delete(
            @Parameter(description = "流程分类ID", required = true)
            @RequestParam Long id) {
        return workflowCategoryService.delete(id);
    }
}
