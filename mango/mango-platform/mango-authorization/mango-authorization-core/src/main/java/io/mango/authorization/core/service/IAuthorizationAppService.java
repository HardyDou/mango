package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.FrontendAppRegistryEntity;

import java.util.List;

/**
 * 授权应用入口服务。
 */
public interface IAuthorizationAppService {

    List<AppVO> listByQuery(Object query);

    List<AppVO> listRuntimeApps(AuthorizationQuery query);

    AppRuntimeDescriptorVO runtimeDescriptor(AuthorizationQuery query, String appCode);

    AppVO get(Long appId);

    AppVO getByAppCode(String appCode);

    AppVO getRuntimeApp(AuthorizationQuery query, String appCode);

    /**
     * Creates or updates the logical application and its login contexts without creating a frontend runtime unit.
     */
    Long upsertBaseline(AppCommand command);

    Long create(AppCommand command);

    Boolean update(AppCommand command);

    Boolean delete(Long appId);

    Long saveFrontendAppRegistry(FrontendAppRegistryEntity registry);

    Boolean deleteFrontendAppRegistry(Long registryId);

    Boolean deleteFrontendAppRegistry(String appCode);
}
