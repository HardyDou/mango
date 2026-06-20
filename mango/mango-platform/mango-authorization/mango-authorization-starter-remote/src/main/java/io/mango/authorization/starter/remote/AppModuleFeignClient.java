package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AppModuleApi;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 应用模块远程客户端。
 */
@FeignClient(name = "mango-authorization", contextId = "appModuleFeignClient", path = "/authorization")
public interface AppModuleFeignClient extends AppModuleApi {

    @Override
    @GetMapping("/app-modules")
    R<List<AppModuleVO>> list(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) Integer status);

    @Override
    @PostMapping("/app-modules")
    R<Long> save(@RequestBody AppModuleCommand command);

    @Override
    @DeleteMapping("/app-modules")
    R<Boolean> disable(
            @RequestParam String appCode,
            @RequestParam String moduleCode);

    @Override
    @PostMapping("/app-modules/sync-menus")
    R<Integer> syncMenus(
            @RequestParam String appCode,
            @RequestParam String moduleCode);

    @Override
    @PostMapping("/app-modules/resource-manifests/register")
    R<Integer> registerResourceManifest(@RequestBody AppModuleResourceManifestCommand command);

    @Override
    @GetMapping("/app-modules/runtime-strategies")
    R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String deployProfile);

    @Override
    @PostMapping("/app-modules/runtime-strategies")
    R<Long> saveRuntimeStrategy(@RequestBody FrontendModuleRuntimeStrategyCommand command);
}
