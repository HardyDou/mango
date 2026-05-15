package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.core.service.IWorkflowProcessService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 协同办公流程实例接口。
 */
@RestController
@RequestMapping("/workflow/processes")
@RequiredArgsConstructor
@Tag(name = "协同办公流程实例", description = "流程发起与我发起的流程查询接口")
public class WorkflowProcessController {

    private final IWorkflowProcessService workflowProcessService;

    @PostMapping("/start")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:start")
    @Operation(summary = "发起流程")
    public R<WorkflowProcessInstanceVO> start(@Valid @RequestBody StartWorkflowProcessCommand command) {
        return workflowProcessService.start(command);
    }

    @GetMapping("/initiated")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
    @Operation(summary = "查询我的发起流程")
    public R<PageResult<WorkflowProcessInstanceVO>> initiated(@ParameterObject WorkflowTaskPageQuery query) {
        return workflowProcessService.initiated(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "查询流程实例详情")
    public R<WorkflowProcessDetailVO> detail(@RequestParam String processInstanceId) {
        return workflowProcessService.detail(processInstanceId);
    }
}
