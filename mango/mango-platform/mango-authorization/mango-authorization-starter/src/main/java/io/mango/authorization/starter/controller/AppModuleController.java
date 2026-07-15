package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AppModuleApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.FrontendModuleRuntimeStrategyQuery;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.authorization.core.service.IAppModuleService;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "逻辑应用集成模块", description = "逻辑应用与能力模块绑定管理接口")
public class AppModuleController implements AppModuleApi {

    private final IAppModuleService appModuleService;
    private final IFrontendRuntimeStrategyService runtimeStrategyService;

    @Override
    @GetMapping
    @Operation(summary = "查询应用集成模块", description = "权限接口。按应用编码和状态查询已集成的能力模块")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<AppModuleVO>> list(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode", required = false) String appCode,
            @Parameter(description = "状态") @RequestParam(name = "status", required = false) Integer status) {
        return R.ok(appModuleService.list(appCode, status));
    }

    @Override
    @PostMapping
    @Operation(summary = "保存应用集成模块", description = "权限接口。创建或更新应用与能力模块的绑定")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> save(@RequestBody AppModuleCommand command) {
        return R.ok(appModuleService.save(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "停用应用集成模块", description = "权限接口。按应用编码和模块编码停用已有绑定")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> disable(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode") String appCode,
            @Parameter(description = "模块编码") @RequestParam(name = "moduleCode") String moduleCode) {
        return R.ok(appModuleService.disableRequired(appCode, moduleCode));
    }

    @Override
    @PostMapping("/sync-menus")
    @Operation(summary = "同步模块菜单到应用菜单资源池", description = "权限接口。将指定能力模块的菜单同步到逻辑应用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:edit")
    public R<Integer> syncMenus(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode") String appCode,
            @Parameter(description = "模块编码") @RequestParam(name = "moduleCode") String moduleCode) {
        return R.ok(appModuleService.syncMenus(appCode, moduleCode));
    }

    @Override
    @PostMapping("/resource-manifests/register")
    @Operation(summary = "注册应用模块资源清单", description = "权限接口。注册应用模块的菜单、权限和运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:menu:edit")
    public R<Integer> registerResourceManifest(@RequestBody AppModuleResourceManifestCommand command) {
        return R.ok(appModuleService.registerResourceManifest(command));
    }

    @Override
    @GetMapping("/runtime-strategies")
    @Operation(summary = "查询应用模块运行策略", description = "权限接口。按应用编码和部署配置档查询前端运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(
            @Parameter(description = "应用编码") @RequestParam(name = "appCode", required = false) String appCode,
            @Parameter(description = "部署配置档") @RequestParam(name = "deployProfile", required = false) String deployProfile) {
        FrontendModuleRuntimeStrategyQuery query = new FrontendModuleRuntimeStrategyQuery();
        query.setAppCode(appCode);
        query.setDeployProfile(deployProfile);
        return R.ok(runtimeStrategyService.list(query));
    }

    @Override
    @PostMapping("/runtime-strategies")
    @Operation(summary = "保存应用模块运行策略", description = "权限接口。新增或更新应用模块的前端运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> saveRuntimeStrategy(@RequestBody FrontendModuleRuntimeStrategyCommand command) {
        return R.ok(runtimeStrategyService.save(command));
    }
}
