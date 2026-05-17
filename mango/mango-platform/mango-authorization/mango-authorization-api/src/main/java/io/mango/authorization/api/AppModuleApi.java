package io.mango.authorization.api;

import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * 逻辑应用集成模块 API。
 */
public interface AppModuleApi {

    R<List<AppModuleVO>> list(String appCode, Integer status);

    R<Long> save(AppModuleCommand command);

    R<Boolean> disable(String appCode, String moduleCode);

    R<Integer> syncMenus(String appCode, String moduleCode);

    R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(String appCode, String deployProfile);

    R<Long> saveRuntimeStrategy(FrontendModuleRuntimeStrategyCommand command);
}
