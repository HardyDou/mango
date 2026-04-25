package io.mango.authorization.starter.controller;

import io.mango.common.result.R;
import io.mango.infra.security.api.ITokenService;
import io.mango.infra.security.core.TokenContextHolder;
import io.mango.authorization.api.SysMenuApi;
import io.mango.authorization.api.po.SysMenu;
import io.mango.authorization.api.vo.SysMenuVO;
import io.mango.authorization.core.service.ISysMenuService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器（权限模块）
 */
@RestController("permissionSysMenuController")
@RequestMapping("/authorization/sys/menu")
@RequiredArgsConstructor
@Tag(name = "系统菜单-权限", description = "菜单管理权限相关接口")
public class SysMenuController implements SysMenuApi {

    private final ISysMenuService sysMenuService;
    private final ISubjectAuthorityService subjectAuthorityService;
    private final ITokenService tokenService;

    @Override
    @GetMapping
    @Operation(summary = "获取当前用户菜单", description = "获取当前用户的菜单树")
    public R<List<SysMenuVO>> getUserMenus(
            @Parameter(description = "菜单类型") @RequestParam(required = false) Integer type,
            @Parameter(description = "父菜单ID") @RequestParam(defaultValue = "0") Long parentId) {
        Long userId = getCurrentUserId();
        List<SysMenuVO> menus = sysMenuService.getUserMenus(type, parentId, userId);
        return R.ok(menus);
    }

    /**
     * Get current authenticated user ID from token context
     */
    private Long getCurrentUserId() {
        String token = TokenContextHolder.getToken();
        if (token == null) {
            return null;
        }
        return tokenService.getUserId(token);
    }

    @Override
    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取所有菜单的树形结构")
    public R<List<SysMenuVO>> getTreeMenus(
            @Parameter(description = "父菜单ID") @RequestParam(name = "parentId", required = false) Long parentId,
            @Parameter(description = "菜单名称") @RequestParam(name = "menuName", required = false) String menuName) {
        List<SysMenuVO> menus = sysMenuService.getTreeMenus(parentId, menuName);
        return R.ok(menus);
    }

    @GetMapping("/{menuId}")
    @Operation(summary = "获取菜单详情")
    public R<SysMenu> getById(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        io.mango.authorization.core.entity.SysMenu menu = sysMenuService.getById(menuId);
        if (menu == null) {
            return R.fail(404, "菜单不存在");
        }
        return R.ok(convertToApiPo(menu));
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        return subjectAuthorityService.listSubjectPermissions(userId).stream().collect(Collectors.toSet());
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    public R<Void> add(@RequestBody io.mango.authorization.api.po.SysMenu menuPo) {
        io.mango.authorization.core.entity.SysMenu menu = convertToEntity(menuPo);
        boolean success = sysMenuService.addMenu(menu);
        return success ? R.ok() : R.fail("添加失败");
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "更新菜单")
    public R<Void> update(
            @Parameter(description = "菜单ID") @PathVariable Long menuId,
            @RequestBody io.mango.authorization.api.po.SysMenu menuPo) {
        io.mango.authorization.core.entity.SysMenu menu = convertToEntity(menuPo);
        boolean success = sysMenuService.updateMenu(menuId, menu);
        return success ? R.ok() : R.fail("更新失败");
    }

    @DeleteMapping("/{menuId}")
    @Operation(summary = "删除菜单")
    public R<Void> delete(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        boolean success = sysMenuService.deleteMenu(menuId);
        return success ? R.ok() : R.fail("删除失败");
    }

    /**
     * Convert core entity to API PO
     */
    private SysMenu convertToApiPo(io.mango.authorization.core.entity.SysMenu entity) {
        if (entity == null) {
            return null;
        }
        SysMenu po = new SysMenu();
        po.setMenuId(entity.getMenuId());
        po.setGroupId(entity.getGroupId());
        po.setParentId(entity.getParentId());
        po.setMenuType(entity.getMenuType());
        po.setMenuName(entity.getMenuName());
        po.setMenuCode(entity.getMenuCode());
        po.setPath(entity.getPath());
        po.setIcon(entity.getIcon());
        po.setSort(entity.getSort());
        po.setStatus(entity.getStatus());
        po.setVisible(entity.getVisible());
        po.setComponent(entity.getComponent());
        po.setKeepAlive(entity.getKeepAlive());
        po.setEmbedded(entity.getEmbedded());
        po.setRedirect(entity.getRedirect());
        po.setPermissions(entity.getPermissions());
        po.setCreateBy(entity.getCreateBy());
        po.setUpdateBy(entity.getUpdateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateTime(entity.getUpdateTime());
        po.setRemark(entity.getRemark());
        po.setDelFlag(entity.getDelFlag());
        return po;
    }

    /**
     * Convert API PO to core entity
     */
    private io.mango.authorization.core.entity.SysMenu convertToEntity(io.mango.authorization.api.po.SysMenu po) {
        if (po == null) {
            return null;
        }
        io.mango.authorization.core.entity.SysMenu entity = new io.mango.authorization.core.entity.SysMenu();
        entity.setMenuId(po.getMenuId());
        entity.setGroupId(po.getGroupId());
        entity.setParentId(po.getParentId());
        entity.setMenuType(po.getMenuType());
        entity.setMenuName(po.getMenuName());
        entity.setMenuCode(po.getMenuCode());
        entity.setPath(po.getPath());
        entity.setIcon(po.getIcon());
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        entity.setVisible(po.getVisible());
        entity.setComponent(po.getComponent());
        entity.setKeepAlive(po.getKeepAlive());
        entity.setEmbedded(po.getEmbedded());
        entity.setRedirect(po.getRedirect());
        entity.setPermissions(po.getPermissions());
        entity.setCreateBy(po.getCreateBy());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateTime(po.getUpdateTime());
        entity.setRemark(po.getRemark());
        entity.setDelFlag(po.getDelFlag());
        return entity;
    }
}
