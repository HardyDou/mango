package io.mango.authorization.api;

import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 逻辑应用集成模块 API。
 */
public interface AppModuleApi {

    R<List<AppModuleVO>> list(@NotBlank String appCode, @Min(0) @Max(1) Integer status);

    R<Long> save(@Valid AppModuleCommand command);

    R<Boolean> disable(@NotBlank String appCode, @NotBlank String moduleCode);

    R<Integer> syncMenus(@NotBlank String appCode, @NotBlank String moduleCode);

    R<Integer> registerResourceManifest(@Valid AppModuleResourceManifestCommand command);

    R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(
            @NotBlank String appCode,
            @NotBlank String deployProfile);

    R<Long> saveRuntimeStrategy(@Valid FrontendModuleRuntimeStrategyCommand command);
}
