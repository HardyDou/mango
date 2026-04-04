package io.mango.auth.starter.controller;

import io.mango.auth.api.SysRoleApi;
import io.mango.auth.api.po.SysRolePo;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleApi sysRoleApi;

    @GetMapping("/list")
    @Perm("auth:role:list")
    public R<List<SysRoleVO>> list() {
        return sysRoleApi.list();
    }

    @GetMapping("/{id}")
    @Perm("auth:role:query")
    public R<SysRoleVO> get(@PathVariable Long id) {
        return sysRoleApi.get(id);
    }

    @PostMapping
    @Perm("auth:role:add")
    public R<Long> create(@RequestBody @Valid SysRolePo po) {
        return sysRoleApi.create(po);
    }

    @PutMapping
    @Perm("auth:role:edit")
    public R<Boolean> update(@RequestBody @Valid SysRolePo po) {
        return sysRoleApi.update(po);
    }

    @DeleteMapping("/{id}")
    @Perm("auth:role:delete")
    public R<Boolean> delete(@PathVariable Long id) {
        return sysRoleApi.delete(id);
    }

    @GetMapping("/{id}/menus")
    @Perm("auth:role:query")
    public R<List<Long>> getRoleMenus(@PathVariable Long id) {
        return sysRoleApi.getRoleMenuIds(id);
    }

    @PutMapping("/{id}/menus")
    @Perm("auth:role:authorize")
    public R<Boolean> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        return sysRoleApi.assignMenus(id, menuIds);
    }
}
