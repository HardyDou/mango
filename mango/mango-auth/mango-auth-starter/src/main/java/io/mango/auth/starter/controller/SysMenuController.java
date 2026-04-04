package io.mango.auth.starter.controller;

import io.mango.auth.api.po.SysMenuPo;
import io.mango.auth.core.service.ISysMenuService;
import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("authSysMenuController")
@RequestMapping("/auth/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService menuService;

    @PostMapping
    @Perm("auth:menu:add")
    public R<Long> addMenu(@RequestBody @Valid SysMenuPo po) {
        return R.ok(menuService.addMenu(po));
    }

    @PutMapping
    @Perm("auth:menu:edit")
    public R<Boolean> updateMenu(@RequestBody @Valid SysMenuPo po) {
        return R.ok(menuService.updateMenu(po));
    }

    @DeleteMapping("/{id}")
    @Perm("auth:menu:delete")
    public R<Boolean> deleteMenu(@PathVariable Long id) {
        return R.ok(menuService.deleteMenu(id));
    }

    @GetMapping("/role/{roleId}")
    @Perm("auth:role:query")
    public R<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        return R.ok(menuService.getRoleMenuIds(roleId));
    }

    @PutMapping("/role/{roleId}")
    @Perm("auth:role:authorize")
    public R<Boolean> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        return R.ok(menuService.assignMenus(roleId, menuIds));
    }
}
