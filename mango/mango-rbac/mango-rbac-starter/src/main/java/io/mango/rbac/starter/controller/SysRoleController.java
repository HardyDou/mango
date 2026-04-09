package io.mango.rbac.starter.controller;

import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import io.mango.rbac.api.SysRoleApi;
import io.mango.rbac.api.po.SysRolePo;
import io.mango.rbac.api.vo.SysRoleVO;
import io.mango.rbac.core.service.ISysRoleService;
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
@RequestMapping("/sys/role")
@RequiredArgsConstructor
@Tag(name = "系统角色-权限", description = "角色管理权限相关接口")
public class SysRoleController implements SysRoleApi {

    private final ISysRoleService sysRoleService;

    @Override
    @GetMapping
    @Operation(summary = "获取角色列表")
    @Perm("rbac:role:list")
    public R<List<SysRoleVO>> list() {
        List<SysRoleVO> roles = sysRoleService.list();
        return R.ok(roles);
    }

    @Override
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    @Perm("rbac:role:query")
    public R<SysRoleVO> get(@Parameter(description = "角色ID") @PathVariable Long id) {
        SysRoleVO role = sysRoleService.get(id);
        if (role == null) {
            return R.fail(404, "角色不存在");
        }
        return R.ok(role);
    }

    @Override
    @PostMapping
    @Operation(summary = "创建角色")
    @Perm("rbac:role:add")
    public R<Long> create(@RequestBody SysRolePo po) {
        Long roleId = sysRoleService.create(po);
        return R.ok(roleId);
    }

    @Override
    @PutMapping
    @Operation(summary = "更新角色")
    @Perm("rbac:role:edit")
    public R<Boolean> update(@RequestBody SysRolePo po) {
        Boolean success = sysRoleService.update(po);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @Perm("rbac:role:delete")
    public R<Boolean> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        Boolean success = sysRoleService.delete(id);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户的角色")
    @Perm("rbac:role:query")
    public R<List<SysRoleVO>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        List<SysRoleVO> roles = sysRoleService.getUserRoles(userId);
        return R.ok(roles);
    }

    @Override
    @PostMapping("/user/{userId}")
    @Operation(summary = "分配角色给用户")
    @Perm("rbac:role:assign")
    public R<Boolean> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody List<Long> roleIds) {
        sysRoleService.assignRoles(userId, roleIds);
        return R.ok(true);
    }

    @Override
    @GetMapping("/{roleId}/menus")
    @Operation(summary = "获取角色的菜单ID列表")
    @Perm("rbac:role:query")
    public R<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<Long> menuIds = sysRoleService.getRoleMenuIds(roleId);
        return R.ok(menuIds);
    }

    @Override
    @PostMapping("/{roleId}/menus")
    @Operation(summary = "给角色分配菜单")
    @Perm("rbac:role:assign")
    public R<Boolean> assignMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(roleId, menuIds);
        return R.ok(true);
    }
}
