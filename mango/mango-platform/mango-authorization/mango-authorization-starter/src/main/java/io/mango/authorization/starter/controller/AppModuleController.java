package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AppModuleApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.authorization.core.service.IAppModuleService;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 逻辑应用集成模块控制器。
 */
@RestController
@RequestMapping("/authorization/app-modules")
@RequiredArgsConstructor
@Tag(name = "逻辑应用集成模块", description = "逻辑应用与能力模块绑定管理接口")
public class AppModuleController implements AppModuleApi {

    private final IAppModuleService appModuleService;
    private final IFrontendRuntimeStrategyService runtimeStrategyService;

    @Override
    @GetMapping
    @Operation(summary = "查询应用集成模块")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<AppModuleVO>> list(
            @Parameter(description = "应用编码") @RequestParam(required = false) String appCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return R.ok(appModuleService.list(appCode, status));
    }

    @Override
    @PostMapping
    @Operation(summary = "保存应用集成模块")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> save(@Valid @RequestBody AppModuleCommand command) {
        return R.ok(appModuleService.save(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "停用应用集成模块")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> disable(
            @Parameter(description = "应用编码") @RequestParam String appCode,
            @Parameter(description = "模块编码") @RequestParam String moduleCode) {
        Boolean success = appModuleService.disable(appCode, moduleCode);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "应用集成模块不存在");
    }

    @Override
    @PostMapping("/sync-menus")
    @Operation(summary = "同步模块菜单到应用菜单资源池")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:edit")
    public R<Integer> syncMenus(
            @Parameter(description = "应用编码") @RequestParam String appCode,
            @Parameter(description = "模块编码") @RequestParam String moduleCode) {
        return R.ok(appModuleService.syncMenus(appCode, moduleCode));
    }

    @Override
    @PostMapping("/resource-manifests/register")
    @Operation(summary = "注册应用模块资源清单")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:edit")
    public R<Integer> registerResourceManifest(@Valid @RequestBody AppModuleResourceManifestCommand command) {
        return R.ok(appModuleService.registerResourceManifest(command));
    }

    @Override
    @GetMapping("/runtime-strategies")
    @Operation(summary = "查询应用模块运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(
            @Parameter(description = "应用编码") @RequestParam(required = false) String appCode,
            @Parameter(description = "部署配置档") @RequestParam(required = false) String deployProfile) {
        return R.ok(runtimeStrategyService.list(appCode, deployProfile, null));
    }

    @Override
    @PostMapping("/runtime-strategies")
    @Operation(summary = "保存应用模块运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> saveRuntimeStrategy(@Valid @RequestBody FrontendModuleRuntimeStrategyCommand command) {
        return R.ok(runtimeStrategyService.save(command));
    }
}
