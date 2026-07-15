package io.mango.authorization.starter.controller;

import io.mango.authorization.api.MenuApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.service.IAuthorizationContextService;
import io.mango.authorization.core.service.IMenuService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 菜单管理控制器。 */
@RestController("authorizationMenuController")
@RequestMapping("/authorization/menus")
@RequiredArgsConstructor
@Validated
@Tag(name = "菜单权限", description = "菜单管理权限相关接口")
public class MenuController implements MenuApi {

    private final IMenuService menuService;
    private final IAuthorizationContextService authorizationContextService;

    @Override
    @GetMapping
    @Operation(summary = "查询菜单资源", description = "菜单管理接口。查询菜单资源列表；fmt=tree时返回树形结构")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:list")
    public R<List<MenuVO>> getMenus(@ParameterObject MenuTreeQuery query) {
        return R.ok(menuService.listMenus(query));
    }

    @Override
    @GetMapping("/user")
    @Operation(summary = "查询当前用户菜单", description = "按系统查询当前登录用户有权限访问的菜单；fmt=tree时返回树形结构")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户菜单")
    public R<List<MenuVO>> getUserMenus(@ParameterObject MenuTreeQuery query) {
        return R.ok(menuService.listUserMenus(
                query, authorizationContextService.current(query.getAppCode())));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取菜单详情", description = "权限接口。按菜单ID查询菜单详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:query")
    public R<MenuVO> getById(
            @Parameter(description = "菜单ID") @RequestParam(name = "menuId") Long menuId) {
        return R.ok(menuService.getMenu(menuId));
    }

    @Override
    @PostMapping
    @Operation(summary = "新增菜单", description = "权限接口。创建授权菜单")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:add")
    public R<Void> add(@RequestBody MenuCommand command) {
        return R.ok(menuService.createMenu(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "更新菜单", description = "权限接口。更新授权菜单")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:edit")
    public R<Void> update(@RequestBody MenuCommand command) {
        return R.ok(menuService.updateMenu(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除菜单", description = "权限接口。按菜单ID删除授权菜单")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:delete")
    public R<Void> delete(
            @Parameter(description = "菜单ID") @RequestParam(name = "menuId") Long menuId) {
        return R.ok(menuService.removeMenu(menuId));
    }
}
