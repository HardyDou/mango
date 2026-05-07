package io.mango.authorization.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.authorization.api.RoleApi;
import io.mango.authorization.api.command.AssignRoleMenusCommand;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.authorization.core.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor
@Tag(name = "角色权限", description = "角色管理权限相关接口")
public class RoleController implements RoleApi {

    private final IRoleService roleService;

    @Override
    @GetMapping
    @Operation(summary = "获取角色列表", description = "权限接口。查询当前授权应用的角色列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:list")
    public R<List<RoleVO>> list() {
        List<RoleVO> roles = roleService.list();
        return R.ok(roles);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取角色详情", description = "权限接口。按角色ID查询角色详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<RoleVO> get(@Parameter(description = "角色ID") @RequestParam Long id) {
        RoleVO role = roleService.get(id);
        if (role == null) {
            return R.fail(404, "角色不存在");
        }
        return R.ok(role);
    }

    @Override
    @PostMapping
    @Operation(summary = "创建角色", description = "权限接口。创建授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:add")
    public R<Long> create(@RequestBody RoleCommand command) {
        Long roleId = roleService.create(command);
        return R.ok(roleId);
    }

    @Override
    @PutMapping
    @Operation(summary = "更新角色", description = "权限接口。更新授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:edit")
    public R<Boolean> update(@RequestBody RoleCommand command) {
        Boolean success = roleService.update(command);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除角色", description = "权限接口。按角色ID删除授权角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:delete")
    public R<Boolean> delete(@Parameter(description = "角色ID") @RequestParam Long id) {
        Boolean success = roleService.delete(id);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @GetMapping("/subjects")
    @Operation(summary = "获取主体的角色", description = "权限接口。按主体ID查询已分配角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<List<RoleVO>> getSubjectRoles(
            @Parameter(description = "主体ID") @RequestParam Long subjectId) {
        List<RoleVO> roles = roleService.getSubjectRoles(subjectId);
        return R.ok(roles);
    }

    @Override
    @PostMapping("/subjects")
    @Operation(summary = "分配角色给主体", description = "权限接口。给用户等主体分配角色")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> assignRoles(@RequestBody AssignSubjectRolesCommand command) {
        roleService.assignRoles(command);
        return R.ok(true);
    }

    @Override
    @GetMapping("/menus")
    @Operation(summary = "获取角色的菜单ID列表", description = "权限接口。按角色ID查询已分配菜单ID列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @RequestParam Long roleId) {
        List<Long> menuIds = roleService.getRoleMenuIds(roleId);
        return R.ok(menuIds);
    }

    @Override
    @PostMapping("/menus")
    @Operation(summary = "给角色分配菜单", description = "权限接口。给角色分配菜单权限")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> assignMenus(@RequestBody AssignRoleMenusCommand command) {
        roleService.assignMenus(command.getRoleId(), command.getMenuIds());
        return R.ok(true);
    }
}
