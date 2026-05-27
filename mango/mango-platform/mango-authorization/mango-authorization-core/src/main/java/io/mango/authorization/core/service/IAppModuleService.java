package io.mango.authorization.core.service;

import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.vo.AppModuleVO;

import java.util.List;

/**
 * 逻辑应用集成模块服务。
 */
public interface IAppModuleService {

    List<AppModuleVO> list(String appCode, Integer status);

    Long save(AppModuleCommand command);

    Boolean disable(String appCode, String moduleCode);

    Integer syncMenus(String appCode, String moduleCode);

    Integer registerResourceManifest(AppModuleResourceManifestCommand command);
}
