package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowDefinitionFromTemplateCommand;
import io.mango.workflow.api.command.CreateWorkflowTemplateFromDefinitionCommand;
import io.mango.workflow.api.command.ImportWorkflowTemplatesCommand;
import io.mango.workflow.api.command.PushWorkflowTemplatesCommand;
import io.mango.workflow.api.command.SaveWorkflowTemplateCommand;
import io.mango.workflow.api.query.WorkflowTemplatePageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateImportVO;
import io.mango.workflow.api.vo.WorkflowTemplateVO;
import io.mango.workflow.core.service.IWorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流模板管理接口。
 */
@RestController
@RequestMapping("/workflow/templates")
@RequiredArgsConstructor
@Tag(name = "工作流模板", description = "流程模板列表、详情、生成模板和由模板导入流程接口")
public class WorkflowTemplateController {

    private final IWorkflowTemplateService workflowTemplateService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:list")
    @Operation(summary = "分页查询流程模板", description = "权限接口。模板是不可直接运行的设计资产")
    public R<PageResult<WorkflowTemplateVO>> page(@ParameterObject WorkflowTemplatePageQuery query) {
        return workflowTemplateService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:query")
    @Operation(summary = "获取流程模板详情", description = "权限接口。查询模板快照配置")
    public R<WorkflowTemplateVO> get(
            @Parameter(description = "流程模板ID", required = true)
            @RequestParam Long id) {
        return workflowTemplateService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:add")
    @Operation(summary = "新增流程模板", description = "权限接口。模板创建后不可修改，只能重新生成新版本")
    public R<String> create(
            @Parameter(description = "保存流程模板命令", required = true)
            @Valid @RequestBody SaveWorkflowTemplateCommand command) {
        return workflowTemplateService.create(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:delete")
    @Operation(summary = "删除流程模板", description = "权限接口。删除未被使用的模板快照")
    public R<Boolean> delete(
            @Parameter(description = "流程模板ID", required = true)
            @RequestParam Long id) {
        return workflowTemplateService.delete(id);
    }

    @PostMapping("/from-definition")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:add")
    @Operation(summary = "流程转模板", description = "权限接口。将流程定义当前设计态复制为不可变模板版本")
    public R<String> createFromDefinition(
            @Parameter(description = "从流程定义创建模板命令", required = true)
            @Valid @RequestBody CreateWorkflowTemplateFromDefinitionCommand command) {
        return workflowTemplateService.createFromDefinition(command);
    }

    @PostMapping("/create-definition")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:create-definition")
    @Operation(summary = "模板导入流程", description = "权限接口。模板不可直接运行，只能导入为租户自己的流程定义草稿")
    public R<String> createDefinition(
            @Parameter(description = "从模板创建流程定义命令", required = true)
            @Valid @RequestBody CreateWorkflowDefinitionFromTemplateCommand command) {
        return workflowTemplateService.createDefinition(command);
    }

    @PostMapping("/import")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:create-definition")
    @Operation(summary = "批量导入流程模板", description = "权限接口。支持按模板分类或按模板ID列表批量导入为租户流程定义草稿")
    public R<WorkflowTemplateImportVO> importTemplates(
            @Parameter(description = "批量导入流程模板命令", required = true)
            @Valid @RequestBody ImportWorkflowTemplatesCommand command) {
        return workflowTemplateService.importTemplates(command);
    }

    @PostMapping("/push")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:template:push")
    @Operation(summary = "推送流程模板", description = "权限接口。将模板推送到目标租户，生成目标租户自己的流程定义草稿")
    public R<WorkflowTemplateImportVO> pushTemplates(
            @Parameter(description = "推送流程模板命令", required = true)
            @Valid @RequestBody PushWorkflowTemplatesCommand command) {
        return workflowTemplateService.pushTemplates(command);
    }
}
