package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;

/** 当前请求授权上下文服务。 */
public interface IAuthorizationContextService {

    AuthorizationQuery current(String appCode);
}
