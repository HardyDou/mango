package io.mango.authorization.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/menu-packages")
@RequiredArgsConstructor
@Tag(name = "菜单授权套餐", description = "菜单授权套餐维护接口")
public class MenuPackageController {

    private final IMenuPackageService menuPackageService;

    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu-package:list")
    @Operation(summary = "查询套餐列表", description = "权限接口。查询菜单授权套餐列表")
    public R<List<MenuPackageVO>> list(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(menuPackageService.listPackages(appCode, keyword, status));
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu-package:query")
    @Operation(summary = "查询套餐详情", description = "权限接口。返回套餐主档和菜单ID集合")
    public R<MenuPackageVO> detail(@Parameter(description = "套餐ID") @RequestParam Long packageId) {
        MenuPackageVO detail = menuPackageService.getById(packageId);
        return detail == null ? R.fail(404, "套餐不存在") : R.ok(detail);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu-package:add")
    @Operation(summary = "新增套餐", description = "权限接口。创建菜单授权套餐")
    public R<Long> create(@RequestBody @Valid MenuPackageCommand command) {
        return R.ok(menuPackageService.create(command));
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu-package:edit")
    @Operation(summary = "修改套餐", description = "权限接口。更新菜单授权套餐")
    public R<Boolean> update(@RequestBody @Valid MenuPackageCommand command) {
        return R.ok(menuPackageService.update(command));
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu-package:delete")
    @Operation(summary = "删除套餐", description = "权限接口。删除菜单授权套餐")
    public R<Boolean> delete(@Parameter(description = "套餐ID") @RequestParam Long packageId) {
        return R.ok(menuPackageService.delete(packageId));
    }
}
