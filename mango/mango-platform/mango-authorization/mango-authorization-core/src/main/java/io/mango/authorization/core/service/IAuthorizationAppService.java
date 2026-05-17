package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.infra.persistence.api.crud.MangoCrudService;

import java.util.List;

/**
 * 授权应用入口服务。
 */
public interface IAuthorizationAppService extends MangoCrudService {

    List<AppVO> listByQuery(Object query);

    List<AppVO> listRuntimeApps(AuthorizationQuery query);

    AppRuntimeDescriptorVO runtimeDescriptor(AuthorizationQuery query, String appCode);

    AppVO get(Long appId);

    AppVO getByAppCode(String appCode);

    Long create(AppCommand command);

    Boolean update(AppCommand command);

    Boolean delete(Long appId);
}
