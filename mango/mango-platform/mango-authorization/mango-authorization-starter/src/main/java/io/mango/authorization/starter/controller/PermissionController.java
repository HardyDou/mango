package io.mango.authorization.starter.controller;

import io.mango.authorization.api.PermissionApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.service.IMenuService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/authorization/permissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "权限码", description = "权限码查询接口")
public class PermissionController implements PermissionApi {

    private final IMenuService menuService;

    @Override
    @GetMapping
    @Operation(summary = "查询全部权限码", description = "权限接口。查询所有启用菜单与按钮声明的权限码")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:permission:list")
    public R<Set<String>> getAllPermissionCodes() {
        return R.ok(menuService.listAllPermissionCodes());
    }
}
