package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AppApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.authorization.core.service.IAuthorizationContextService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

/**
 * 授权应用管理控制器。
 */
@RestController
@RequestMapping("/authorization/apps")
@RequiredArgsConstructor
@Validated
@Tag(name = "授权应用", description = "授权应用与前端运行配置管理接口")
public class AppController implements AppApi {

    private final IAuthorizationAppService appService;
    private final IAuthorizationContextService authorizationContextService;

    @Override
    @GetMapping
    @Operation(summary = "获取应用列表", description = "权限接口。查询授权应用及前端运行配置列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<AppVO>> list() {
        return R.ok(appService.listByQuery(null));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取应用详情", description = "权限接口。按应用ID查询授权应用及前端运行配置详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:query")
    public R<AppVO> get(@Parameter(description = "应用ID") @RequestParam(name = "appId") Long appId) {
        return R.ok(appService.get(appId));
    }

    @Override
    @GetMapping("/runtime")
    @Operation(summary = "获取应用运行配置", description = "登录接口。返回当前租户和主体可访问的前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询应用运行配置")
    public R<List<AppVO>> runtime() {
        return R.ok(appService.listRuntimeApps(authorizationContextService.current(null)));
    }

    @Override
    @GetMapping("/runtime/descriptor")
    @Operation(summary = "获取前端运行描述", description = "登录接口。返回当前部署配置档、运行单元和模块运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询前端运行描述")
    public R<AppRuntimeDescriptorVO> runtimeDescriptor(
            @Parameter(description = "逻辑应用编码")
            @RequestParam(name = "appCode", defaultValue = "internal-admin") String appCode) {
        return R.ok(appService.runtimeDescriptor(authorizationContextService.current(appCode), appCode));
    }

    @Override
    @GetMapping("/runtime/detail")
    @Operation(summary = "获取单个应用运行配置", description = "登录接口。按应用编码返回当前主体可访问的前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询单个应用运行配置")
    public R<AppVO> runtimeDetail(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode") String appCode) {
        return R.ok(appService.getRuntimeApp(authorizationContextService.current(appCode), appCode));
    }

    @Override
    @PostMapping
    @Operation(summary = "创建应用", description = "权限接口。创建授权应用及前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:add")
    public R<Long> create(@RequestBody AppCommand command) {
        return R.ok(appService.create(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "更新应用", description = "权限接口。更新授权应用及前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> update(@RequestBody AppCommand command) {
        return R.ok(appService.update(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除应用", description = "权限接口。按应用ID删除授权应用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:delete")
    public R<Boolean> delete(@Parameter(description = "应用ID") @RequestParam(name = "appId") Long appId) {
        return R.ok(appService.delete(appId));
    }
}
