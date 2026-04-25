package io.mango.authorization.starter.controller;

import io.mango.infra.security.api.Perm;
import io.mango.common.result.R;
import io.mango.authorization.api.SysRoleApi;
import io.mango.authorization.api.po.SysRolePo;
import io.mango.authorization.api.vo.SysRoleVO;
import io.mango.authorization.core.service.ISysRoleService;
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
@RequestMapping("/authorization/sys/role")
@RequiredArgsConstructor
@Tag(name = "系统角色-权限", description = "角色管理权限相关接口")
public class SysRoleController implements SysRoleApi {

    private final ISysRoleService sysRoleService;

    @Override
    @GetMapping
    @Operation(summary = "获取角色列表")
    @Perm("authorization:role:list")
    public R<List<SysRoleVO>> list() {
        List<SysRoleVO> roles = sysRoleService.list();
        return R.ok(roles);
    }

    @Override
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    @Perm("authorization:role:query")
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
    @Perm("authorization:role:add")
    public R<Long> create(@RequestBody SysRolePo po) {
        Long roleId = sysRoleService.create(po);
        return R.ok(roleId);
    }

    @Override
    @PutMapping
    @Operation(summary = "更新角色")
    @Perm("authorization:role:edit")
    public R<Boolean> update(@RequestBody SysRolePo po) {
        Boolean success = sysRoleService.update(po);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @Perm("authorization:role:delete")
    public R<Boolean> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        Boolean success = sysRoleService.delete(id);
        return success ? R.ok(true) : R.fail(404, "角色不存在");
    }

    @Override
    @GetMapping("/subjects/{subjectId}")
    @Operation(summary = "获取主体的角色")
    @Perm("authorization:role:query")
    public R<List<SysRoleVO>> getUserRoles(
            @Parameter(description = "主体ID") @PathVariable Long subjectId) {
        List<SysRoleVO> roles = sysRoleService.getSubjectRoles(subjectId);
        return R.ok(roles);
    }

    @Override
    @PostMapping("/subjects/{subjectId}")
    @Operation(summary = "分配角色给主体")
    @Perm("authorization:role:assign")
    public R<Boolean> assignRoles(
            @Parameter(description = "主体ID") @PathVariable Long subjectId,
            @RequestBody List<Long> roleIds) {
        sysRoleService.assignRoles(subjectId, roleIds);
        return R.ok(true);
    }

    @Override
    @GetMapping("/{roleId}/menus")
    @Operation(summary = "获取角色的菜单ID列表")
    @Perm("authorization:role:query")
    public R<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<Long> menuIds = sysRoleService.getRoleMenuIds(roleId);
        return R.ok(menuIds);
    }

    @Override
    @PostMapping("/{roleId}/menus")
    @Operation(summary = "给角色分配菜单")
    @Perm("authorization:role:assign")
    public R<Boolean> assignMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(roleId, menuIds);
        return R.ok(true);
    }
}
