package io.mango.authorization.starter.controller;

import io.mango.common.result.R;
import io.mango.infra.security.api.ITokenProvider;
import io.mango.infra.security.core.TokenContextHolder;
import io.mango.authorization.api.MenuApi;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.service.IMenuService;
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
 * 菜单管理控制器。
 */
@RestController("authorizationMenuController")
@RequestMapping("/authorization/menus")
@RequiredArgsConstructor
@Tag(name = "菜单权限", description = "菜单管理权限相关接口")
public class MenuController implements MenuApi {

    private final IMenuService menuService;
    private final ISubjectAuthorityService subjectAuthorityService;
    private final ITokenProvider tokenService;

    @GetMapping
    @Operation(summary = "获取当前用户菜单", description = "获取当前用户的菜单树")
    public R<List<MenuVO>> getUserMenus(
            @Parameter(description = "应用编码") @RequestParam(required = false) String appCode,
            @Parameter(description = "菜单类型") @RequestParam(required = false) Integer type,
            @Parameter(description = "父菜单ID") @RequestParam(defaultValue = "0") Long parentId) {
        Long userId = getCurrentUserId();
        List<MenuVO> menus = menuService.getUserMenus(appCode, type, parentId, userId);
        return R.ok(menus);
    }

    @Override
    public R<List<MenuVO>> getUserMenus(MenuTreeQuery query) {
        Long parentId = query.getParentId() == null ? 0L : query.getParentId();
        return getUserMenus(query.getAppCode(), query.getType(), parentId);
    }

    /**
     * 从 token 上下文读取当前用户 ID。
     */
    private Long getCurrentUserId() {
        String token = TokenContextHolder.getToken();
        if (token == null) {
            return null;
        }
        return tokenService.getUserId(token);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取所有菜单的树形结构")
    public R<List<MenuVO>> getTreeMenus(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode", required = false) String appCode,
            @Parameter(description = "父菜单ID") @RequestParam(name = "parentId", required = false) Long parentId,
            @Parameter(description = "菜单名称") @RequestParam(name = "menuName", required = false) String menuName) {
        List<MenuVO> menus = menuService.getTreeMenus(appCode, parentId, menuName);
        return R.ok(menus);
    }

    @Override
    public R<List<MenuVO>> getTreeMenus(MenuTreeQuery query) {
        return getTreeMenus(query.getAppCode(), query.getParentId(), query.getMenuName());
    }

    @Override
    @GetMapping("/{menuId}")
    @Operation(summary = "获取菜单详情")
    public R<MenuVO> getById(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        io.mango.authorization.core.entity.Menu menu = menuService.getById(menuId);
        if (menu == null) {
            return R.fail(404, "菜单不存在");
        }
        return R.ok(convertToVO(menu));
    }

    @Override
    public R<Set<String>> getUserPermissions(Long userId) {
        return R.ok(subjectAuthorityService.listSubjectPermissions(userId).stream().collect(Collectors.toSet()));
    }

    @Override
    @PostMapping
    @Operation(summary = "新增菜单")
    public R<Void> add(@RequestBody MenuCommand command) {
        io.mango.authorization.core.entity.Menu menu = convertToEntity(command);
        boolean success = menuService.addMenu(menu);
        return success ? R.ok() : R.fail("添加失败");
    }

    @Override
    @PutMapping("/{menuId}")
    @Operation(summary = "更新菜单")
    public R<Void> update(
            @Parameter(description = "菜单ID") @PathVariable Long menuId,
            @RequestBody MenuCommand command) {
        io.mango.authorization.core.entity.Menu menu = convertToEntity(command);
        boolean success = menuService.updateMenu(menuId, menu);
        return success ? R.ok() : R.fail("更新失败");
    }

    @Override
    @DeleteMapping("/{menuId}")
    @Operation(summary = "删除菜单")
    public R<Void> delete(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        boolean success = menuService.deleteMenu(menuId);
        return success ? R.ok() : R.fail("删除失败");
    }

    /**
     * 将核心实体转换为 API VO。
     */
    private MenuVO convertToVO(io.mango.authorization.core.entity.Menu entity) {
        if (entity == null) {
            return null;
        }
        MenuVO po = new MenuVO();
        po.setMenuId(entity.getMenuId());
        po.setAppCode(entity.getAppCode());
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
        return po;
    }

    /**
     * 将 API 命令转换为核心实体。
     */
    private io.mango.authorization.core.entity.Menu convertToEntity(MenuCommand po) {
        if (po == null) {
            return null;
        }
        io.mango.authorization.core.entity.Menu entity = new io.mango.authorization.core.entity.Menu();
        entity.setMenuId(po.getMenuId());
        entity.setAppCode(po.getAppCode());
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
