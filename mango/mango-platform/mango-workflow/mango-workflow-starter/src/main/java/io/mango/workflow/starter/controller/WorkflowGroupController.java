package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowGroupCommand;
import io.mango.workflow.api.query.WorkflowGroupPageQuery;
import io.mango.workflow.api.vo.WorkflowGroupVO;
import io.mango.workflow.core.service.IWorkflowGroupService;
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
 * 工作流分组管理接口。
 */
@RestController
@RequestMapping("/workflow/groups")
@RequiredArgsConstructor
@Tag(name = "工作流分组", description = "流程分组列表、详情、新增、修改与删除接口")
public class WorkflowGroupController {

    private final IWorkflowGroupService workflowGroupService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:list")
    @Operation(summary = "分页查询流程分组", description = "权限接口。分页查询流程分组配置")
    public R<PageResult<WorkflowGroupVO>> page(@ParameterObject WorkflowGroupPageQuery query) {
        return workflowGroupService.page(query);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:list")
    @Operation(summary = "查询流程分组选项", description = "权限接口。查询流程分组轻量列表，用于流程定义筛选和表单选择")
    public R<List<WorkflowGroupVO>> list(
            @Parameter(description = "状态：0-停用，1-启用")
            @RequestParam(required = false) Integer status) {
        return workflowGroupService.list(status);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:query")
    @Operation(summary = "获取流程分组详情", description = "权限接口。按流程分组ID查询详情")
    public R<WorkflowGroupVO> get(
            @Parameter(description = "流程分组ID", required = true)
            @RequestParam Long id) {
        return workflowGroupService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:add")
    @Operation(summary = "新增流程分组", description = "权限接口。创建流程分组")
    public R<String> create(
            @Parameter(description = "保存流程分组命令", required = true)
            @Valid @RequestBody SaveWorkflowGroupCommand command) {
        return workflowGroupService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:edit")
    @Operation(summary = "修改流程分组", description = "权限接口。更新流程分组")
    public R<Boolean> update(
            @Parameter(description = "保存流程分组命令", required = true)
            @Valid @RequestBody SaveWorkflowGroupCommand command) {
        return workflowGroupService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:workflow:delete")
    @Operation(summary = "删除流程分组", description = "权限接口。删除未被流程定义引用的流程分组")
    public R<Boolean> delete(
            @Parameter(description = "流程分组ID", required = true)
            @RequestParam Long id) {
        return workflowGroupService.delete(id);
    }
}
