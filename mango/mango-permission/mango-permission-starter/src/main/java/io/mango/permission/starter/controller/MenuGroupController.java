package io.mango.permission.starter.controller;

import io.mango.permission.api.MenuGroupApi;
import io.mango.permission.api.vo.SysMenuGroupVO;
import io.mango.permission.core.service.ISysMenuGroupService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Menu Group Controller
 * Implements MenuGroupApi
 *
 * @author Mango
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "菜单分组管理", description = "系统菜单分组增删改查")
public class MenuGroupController implements MenuGroupApi {

    private final ISysMenuGroupService menuGroupService;

    @Override
    @GetMapping("/permission/menu/group")
    @Operation(summary = "获取所有菜单分组")
    public R<List<SysMenuGroupVO>> listMenuGroups() {
        return R.ok(menuGroupService.listMenuGroups());
    }

    @Override
    @GetMapping("/permission/menu/group/{groupId}")
    @Operation(summary = "获取菜单分组详情")
    public R<SysMenuGroupVO> getMenuGroup(@PathVariable(name = "groupId") Long groupId) {
        SysMenuGroupVO group = menuGroupService.getMenuGroup(groupId);
        return group != null ? R.ok(group) : R.fail("分组不存在");
    }

    @Override
    @PostMapping("/permission/menu/group")
    @Operation(summary = "创建菜单分组")
    public R<Long> createMenuGroup(@RequestBody SysMenuGroupVO menuGroup) {
        Long groupId = menuGroupService.createMenuGroup(menuGroup);
        return R.ok(groupId);
    }

    @Override
    @PutMapping("/permission/menu/group")
    @Operation(summary = "更新菜单分组")
    public R<Void> updateMenuGroup(@RequestBody SysMenuGroupVO menuGroup) {
        menuGroupService.updateMenuGroup(menuGroup);
        return R.ok();
    }

    @Override
    @DeleteMapping("/permission/menu/group/{groupId}")
    @Operation(summary = "删除菜单分组")
    public R<Void> deleteMenuGroup(@PathVariable Long groupId) {
        menuGroupService.deleteMenuGroup(groupId);
        return R.ok();
    }
}
