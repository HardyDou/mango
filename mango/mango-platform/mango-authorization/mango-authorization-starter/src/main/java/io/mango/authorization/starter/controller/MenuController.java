package io.mango.authorization.starter.controller;

import io.mango.common.result.R;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.TokenContextHolder;
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
import org.springdoc.core.annotations.ParameterObject;
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

    @Override
    @GetMapping
    @Operation(summary = "查询菜单资源", description = "菜单管理接口。查询菜单资源列表；fmt=tree 时返回树形结构")
    public R<List<MenuVO>> getMenus(@ParameterObject MenuTreeQuery query) {
        List<MenuVO> menus = menuService.listMenus(
                query.getAppCode(),
                query.getType(),
                query.getParentId(),
                query.getMenuName(),
                query.getStatus(),
                isTreeFormat(query.getFmt()));
        return R.ok(menus);
    }

    @Override
    @GetMapping("/user")
    @Operation(summary = "查询当前用户菜单", description = "按系统查询当前登录用户有权限访问的菜单；fmt=tree 时返回树形结构")
    public R<List<MenuVO>> getUserMenus(@ParameterObject MenuTreeQuery query) {
        Long userId = getCurrentUserId();
        List<MenuVO> menus = menuService.listUserMenus(
                query.getAppCode(),
                query.getType(),
                query.getParentId(),
                userId,
                isTreeFormat(query.getFmt()));
        return R.ok(menus);
    }

    /**
     * 从 token 上下文读取当前用户 ID。
     */
    private Long getCurrentUserId() {
        String token = TokenContextHolder.getToken();
        if (token == null) {
            return null;
        }
        return tokenService.getUserId(stripBearer(token));
    }

    private String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return token.substring(ITokenProvider.BEARER_PREFIX.length());
        }
        return token;
    }

    private boolean isTreeFormat(String fmt) {
        return "tree".equalsIgnoreCase(fmt);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取菜单详情", description = "权限接口。按菜单ID查询菜单详情")
    public R<MenuVO> getById(
            @Parameter(description = "菜单ID") @RequestParam Long menuId) {
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
    @Operation(summary = "新增菜单", description = "权限接口。创建授权菜单")
    public R<Void> add(@RequestBody MenuCommand command) {
        io.mango.authorization.core.entity.Menu menu = convertToEntity(command);
        boolean success = menuService.addMenu(menu);
        return success ? R.ok() : R.fail("添加失败");
    }

    @Override
    @PutMapping
    @Operation(summary = "更新菜单", description = "权限接口。更新授权菜单")
    public R<Void> update(@RequestBody MenuCommand command) {
        io.mango.authorization.core.entity.Menu menu = convertToEntity(command);
        boolean success = menuService.updateMenu(command.getMenuId(), menu);
        return success ? R.ok() : R.fail("更新失败");
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除菜单", description = "权限接口。按菜单ID删除授权菜单")
    public R<Void> delete(
            @Parameter(description = "菜单ID") @RequestParam Long menuId) {
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
