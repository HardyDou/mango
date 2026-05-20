package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowTemplateCategoryCommand;
import io.mango.workflow.api.query.WorkflowTemplateCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateCategoryVO;
import io.mango.workflow.core.service.IWorkflowTemplateCategoryService;
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
 * 工作流模板分类管理接口。
 */
@RestController
@RequestMapping("/workflow/template-categories")
@RequiredArgsConstructor
@Tag(name = "工作流模板分类", description = "流程模板分类列表、详情、新增、修改与删除接口")
public class WorkflowTemplateCategoryController {

    private final IWorkflowTemplateCategoryService workflowTemplateCategoryService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:list")
    @Operation(summary = "分页查询流程模板分类", description = "权限接口。分页查询流程模板分类配置")
    public R<PageResult<WorkflowTemplateCategoryVO>> page(@ParameterObject WorkflowTemplateCategoryPageQuery query) {
        return workflowTemplateCategoryService.page(query);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:list")
    @Operation(summary = "查询流程模板分类选项", description = "权限接口。查询流程模板分类轻量列表，用于模板筛选和表单选择")
    public R<List<WorkflowTemplateCategoryVO>> list(
            @Parameter(description = "状态：0-停用，1-启用")
            @RequestParam(required = false) Integer status) {
        return workflowTemplateCategoryService.list(status);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:query")
    @Operation(summary = "获取流程模板分类详情", description = "权限接口。按流程模板分类ID查询详情")
    public R<WorkflowTemplateCategoryVO> get(
            @Parameter(description = "流程模板分类ID", required = true)
            @RequestParam Long id) {
        return workflowTemplateCategoryService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:add")
    @Operation(summary = "新增流程模板分类", description = "权限接口。创建流程模板分类")
    public R<String> create(
            @Parameter(description = "保存流程模板分类命令", required = true)
            @Valid @RequestBody SaveWorkflowTemplateCategoryCommand command) {
        return workflowTemplateCategoryService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:edit")
    @Operation(summary = "修改流程模板分类", description = "权限接口。更新流程模板分类")
    public R<Boolean> update(
            @Parameter(description = "保存流程模板分类命令", required = true)
            @Valid @RequestBody SaveWorkflowTemplateCategoryCommand command) {
        return workflowTemplateCategoryService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:delete")
    @Operation(summary = "删除流程模板分类", description = "权限接口。删除未被模板引用的流程模板分类")
    public R<Boolean> delete(
            @Parameter(description = "流程模板分类ID", required = true)
            @RequestParam Long id) {
        return workflowTemplateCategoryService.delete(id);
    }
}
