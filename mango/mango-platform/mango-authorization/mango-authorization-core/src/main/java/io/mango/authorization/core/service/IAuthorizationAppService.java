package io.mango.authorization.core.service;

import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppVO;

import java.util.List;

/**
 * 授权应用入口服务。
 */
public interface IAuthorizationAppService {

    List<AppVO> list();

    AppVO get(Long appId);

    Long create(AppCommand command);

    Boolean update(AppCommand command);

    Boolean delete(Long appId);
}
