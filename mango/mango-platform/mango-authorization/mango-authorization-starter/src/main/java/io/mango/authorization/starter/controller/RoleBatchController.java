package io.mango.authorization.starter.controller;

import io.mango.authorization.api.RoleBatchApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.BatchRoleAssignmentCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.BatchRoleAssignmentPreviewVO;
import io.mango.authorization.api.vo.BatchRoleAssignmentResultVO;
import io.mango.authorization.core.service.IRoleService;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 租户范围批量角色绑定控制器。 */
@RestController
@RequestMapping("/authorization/roles/batch")
@RequiredArgsConstructor
@Validated
@Tag(name = "批量角色授权", description = "租户内批量绑定和解除成员角色")
public class RoleBatchController implements RoleBatchApi {

    private final IRoleService roleService;

    @Override
    @PostMapping("/preview")
    @Operation(summary = "预览批量角色目标", description = "权限接口。预览当前租户内符合范围的启用成员，不写入授权关系")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:batch-assign")
    public R<BatchRoleAssignmentPreviewVO> preview(@RequestBody @Valid BatchRoleAssignmentCommand command) {
        return R.ok(roleService.previewBatchRoleAssignment(command));
    }

    @Override
    @PostMapping("/assign")
    @Log("批量绑定成员角色")
    @Operation(summary = "批量绑定成员角色", description = "权限接口。按当前租户和角色上下文幂等绑定启用成员角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:batch-assign")
    public R<BatchRoleAssignmentResultVO> assign(@RequestBody @Valid BatchRoleAssignmentCommand command) {
        return R.ok(roleService.assignBatchRole(command));
    }

    @Override
    @PostMapping("/unassign")
    @Log("批量解除成员角色")
    @Operation(summary = "批量解除成员角色", description = "权限接口。按当前租户和角色上下文批量解除成员角色绑定")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:batch-assign")
    public R<BatchRoleAssignmentResultVO> unassign(@RequestBody @Valid BatchRoleAssignmentCommand command) {
        return R.ok(roleService.unassignBatchRole(command));
    }
}
