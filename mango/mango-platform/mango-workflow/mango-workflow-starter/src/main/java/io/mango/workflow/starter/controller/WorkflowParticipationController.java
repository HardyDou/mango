package io.mango.workflow.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowParticipationApi;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessParticipantsVO;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.api.vo.WorkflowParticipationBusinessVO;
import io.mango.workflow.core.service.IWorkflowParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工作流历史参与关系接口。 */
@RestController
@RequestMapping("/workflow/participations")
@RequiredArgsConstructor
@Validated
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed collaborator is injected")
@Tag(name = "工作流参与关系", description = "历史参与人的只读查询与业务参与人声明")
public class WorkflowParticipationController implements WorkflowParticipationApi {
    private final IWorkflowParticipationService service;

    @Override
    @GetMapping("/access")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "登录用户查询自己的工作流只读参与事实")
    @Operation(summary = "查询参与可读性", description = "查询当前登录用户对指定业务坐标的只读参与事实")
    public R<WorkflowParticipationAccessVO> access(@ParameterObject WorkflowParticipationAccessQuery query) {
        return R.ok(service.access(query));
    }

    @Override
    @GetMapping("/my")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "登录用户分页查询自己的工作流参与业务")
    @Operation(summary = "分页查询我的参与业务", description = "分页查询当前登录用户参与过的业务坐标")
    public R<PageResult<WorkflowParticipationBusinessVO>> my(@ParameterObject WorkflowParticipationPageQuery query) {
        return R.ok(service.my(query));
    }

    @Override
    @PostMapping("/business")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:participation:declare")
    @Operation(summary = "替换业务声明参与人", description = "完整替换指定业务流程实例声明的参与人用户")
    public R<WorkflowBusinessParticipantsVO> replaceBusinessParticipants(
            @RequestBody ReplaceWorkflowBusinessParticipantsCommand command) {
        return R.ok(service.replaceBusinessParticipants(command));
    }
}
