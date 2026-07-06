package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
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

import java.util.Collection;
import java.util.List;

/**
 * 审批中心流程实例接口。
 */
@RestController
@RequestMapping("/workflow/processes")
@RequiredArgsConstructor
@Tag(name = "审批中心流程实例", description = "流程发起与我发起的流程查询接口")
public class WorkflowProcessController implements WorkflowProcessApi, WorkflowBusinessProcessApi {

    private final IWorkflowProcessService workflowProcessService;

    @PostMapping("/start")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:start")
    @Operation(summary = "发起流程")
    @Override
    public R<WorkflowProcessInstanceVO> start(@Valid @RequestBody StartWorkflowProcessCommand command) {
        return workflowProcessService.start(command);
    }

    @PostMapping("/start-business")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:start")
    @Operation(summary = "创建业务申请并发起流程", description = "一次性创建业务申请、发起流程并返回当前任务快照")
    @Override
    public R<WorkflowStartResultVO> startBusinessWorkflow(@Valid @RequestBody StartBusinessWorkflowCommand command) {
        return workflowProcessService.startBusinessWorkflow(command);
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

    @GetMapping("/history")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:process:detail")
    @Operation(summary = "按业务主键查询流程历史")
    public R<PageResult<WorkflowProcessInstanceVO>> history(@RequestParam String businessKey, @ParameterObject WorkflowTaskPageQuery query) {
        return workflowProcessService.historyByBusinessKey(businessKey, query);
    }

    @GetMapping("/business/latest-by-keys")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "按业务主键批量查询最新流程状态")
    @Override
    public List<WorkflowBusinessProcessVO> latestByBusinessKeys(
            @RequestParam("businessKeys") Collection<String> businessKeys) {
        return workflowProcessService.latestByBusinessKeys(businessKeys);
    }

    @GetMapping("/business/latest-by-type-keys")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "按业务类型和业务主键批量查询最新流程状态")
    @Override
    public List<WorkflowBusinessProcessVO> latestByBusinessKeys(@RequestParam("businessType") String businessType,
                                                               @RequestParam("businessKeys") Collection<String> businessKeys) {
        return workflowProcessService.latestByBusinessKeys(businessType, businessKeys);
    }
}
