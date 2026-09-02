package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.annotation.PublicAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.AdminBrandingApi;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;
import io.mango.system.core.service.IAdminBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/admin-branding")
@RequiredArgsConstructor
@Tag(name = "网站配置", description = "网站配置管理接口")
public class AdminBrandingController implements AdminBrandingApi {

    private final IAdminBrandingService brandingService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:admin-branding:query")
    @Operation(summary = "获取网站配置", description = "获取网站配置并返回处理结果")
    public R<AdminBrandingVO> get() {
        return R.ok(brandingService.get());
    }

    @Override
    @GetMapping("/public")
    @PublicAccess(desc = "网站公共配置")
    @Operation(summary = "获取网站公共配置", description = "无需登录，用于登录页和后台框架初始化网站展示信息")
    public R<AdminBrandingVO> publicConfig() {
        return R.ok(brandingService.get());
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:admin-branding:edit")
    @Operation(summary = "保存网站配置", description = "保存网站配置并返回处理结果")
    @Log("保存网站配置")
    public R<Boolean> save(@RequestBody SaveAdminBrandingCommand command) {
        return R.ok(brandingService.save(command));
    }
}
