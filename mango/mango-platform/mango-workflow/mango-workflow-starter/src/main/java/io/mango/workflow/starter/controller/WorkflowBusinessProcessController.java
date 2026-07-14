package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.query.WorkflowBusinessKeysQuery;
import io.mango.workflow.api.query.WorkflowBusinessTypeKeysQuery;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.core.service.IWorkflowProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 业务侧流程状态查询接口。 */
@RestController
@RequestMapping("/workflow/processes")
@RequiredArgsConstructor
@Validated
@Tag(name = "业务流程状态", description = "面向业务模块的流程状态批量查询接口")
public class WorkflowBusinessProcessController implements WorkflowBusinessProcessApi {

    private final IWorkflowProcessService workflowProcessService;

    @GetMapping("/business/latest-by-keys")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "按业务主键批量查询最新流程状态", description = "按业务主键集合查询每项业务的最新流程状态")
    @Override
    public R<List<WorkflowBusinessProcessVO>> latestByBusinessKeys(
            @ParameterObject WorkflowBusinessKeysQuery query) {
        return R.ok(workflowProcessService.latestByBusinessKeys(query.getBusinessKeys()));
    }

    @GetMapping("/business/latest-by-type-keys")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:business-apply:list")
    @Operation(summary = "按业务类型批量查询最新流程状态", description = "按业务类型和业务主键集合查询最新流程状态")
    @Override
    public R<List<WorkflowBusinessProcessVO>> latestByBusinessTypeKeys(
            @ParameterObject WorkflowBusinessTypeKeysQuery query) {
        return R.ok(workflowProcessService.latestByBusinessKeys(
                query.getBusinessType(), query.getBusinessKeys()));
    }
}
