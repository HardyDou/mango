package io.mango.authorization.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.authorization.api.RoleApi;
import io.mango.authorization.api.command.AssignRoleMenusCommand;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.authorization.api.request.SubjectRoleBatchRequest;
import io.mango.authorization.api.vo.SubjectRoleSummaryVO;
import io.mango.authorization.core.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring singleton service is intentionally injected and retained"))
@Validated
@Tag(name = "角色权限", description = "角色管理权限相关接口")
public class RoleController implements RoleApi {

    private final IRoleService roleService;

    @Override
    @GetMapping
    @Operation(summary = "获取角色列表", description = "权限接口。查询当前授权应用的角色列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:list")
    public R<List<RoleVO>> list() {
        return R.ok(roleService.list());
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取角色详情", description = "权限接口。按角色ID查询角色详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<RoleVO> get(@Parameter(description = "角色ID") @RequestParam(name = "id") Long id) {
        return R.ok(roleService.get(id));
    }

    @Override
    @PostMapping
    @Operation(summary = "创建角色", description = "权限接口。创建授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:add")
    public R<Long> create(@RequestBody RoleCommand command) {
        return R.ok(roleService.create(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "更新角色", description = "权限接口。更新授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:edit")
    public R<Boolean> update(@RequestBody RoleCommand command) {
        return R.ok(roleService.update(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除角色", description = "权限接口。按角色ID删除授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:delete")
    public R<Boolean> delete(@Parameter(description = "角色ID") @RequestParam(name = "id") Long id) {
        return R.ok(roleService.delete(id));
    }

    @Override
    @GetMapping("/subjects")
    @Operation(summary = "获取成员的角色", description = "权限接口。按机构成员ID查询已分配角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<List<RoleVO>> getSubjectRoles(
            @Parameter(description = "机构成员ID") @RequestParam(name = "subjectId") Long subjectId) {
        return R.ok(roleService.getSubjectRoles(subjectId));
    }

    @Override
    @PostMapping("/subjects/batch")
    @Operation(summary = "批量获取成员角色", description = "权限接口。批量查询当前租户、当前应用下成员直接分配的有效角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:list")
    public R<List<SubjectRoleSummaryVO>> getSubjectRolesBatch(@RequestBody SubjectRoleBatchRequest request) {
        return R.ok(roleService.getSubjectRolesBatch(request.getSubjectIds()));
    }

    @Override
    @PostMapping("/subjects")
    @Operation(summary = "分配角色给成员", description = "权限接口。给机构成员分配角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> assignRoles(@RequestBody AssignSubjectRolesCommand command) {
        return R.ok(roleService.assignRolesRequired(command));
    }

    @Override
    @GetMapping("/menus")
    @Operation(summary = "获取角色的菜单ID列表", description = "权限接口。按角色ID查询已分配菜单ID列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @RequestParam(name = "roleId") Long roleId) {
        return R.ok(roleService.getRoleMenuIds(roleId));
    }

    @Override
    @GetMapping("/assignable-menus")
    @Operation(summary = "获取可授权菜单树", description = "权限接口。查询当前机构成员可分配给角色的菜单权限范围")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<List<MenuVO>> listAssignableMenus(
            @Parameter(description = "应用编码")
            @RequestParam(name = "appCode", required = false) String appCode) {
        return R.ok(roleService.listAssignableMenus(appCode));
    }

    @Override
    @PostMapping("/menus")
    @Operation(summary = "给角色分配菜单", description = "权限接口。给角色分配菜单权限")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> assignMenus(@RequestBody AssignRoleMenusCommand command) {
        return R.ok(roleService.assignMenusRequired(command.getRoleId(), command.getMenuIds()));
    }
}
