package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;
import io.mango.system.core.service.IAdminBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/admin-branding")
@RequiredArgsConstructor
@Tag(name = "后台品牌配置", description = "Admin 后台自身品牌配置接口")
public class AdminBrandingController {

    private final IAdminBrandingService adminBrandingService;

    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:admin-branding:query")
    @Operation(summary = "获取后台品牌配置", description = "权限接口。后台配置页读取 Admin 品牌配置")
    public R<AdminBrandingVO> get() {
        return adminBrandingService.get();
    }

    @GetMapping("/public")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "后台品牌公共配置")
    @Operation(summary = "获取后台品牌公共配置", description = "公共接口。登录页和后台框架读取 Admin 品牌配置")
    public R<AdminBrandingVO> publicConfig() {
        return adminBrandingService.get();
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:admin-branding:edit")
    @Operation(summary = "保存后台品牌配置", description = "权限接口。保存 Admin 品牌配置")
    @Log("保存后台品牌配置")
    public R<Boolean> save(@RequestBody @Valid SaveAdminBrandingCommand command) {
        return adminBrandingService.save(command);
    }
}
