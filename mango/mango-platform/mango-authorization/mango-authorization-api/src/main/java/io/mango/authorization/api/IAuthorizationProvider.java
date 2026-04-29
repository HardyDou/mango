package io.mango.authorization.api;

/**
 * 本地与远程安全集成都使用的授权提供者。
 */
public interface IAuthorizationProvider {

    AuthorizationSnapshot load(AuthorizationQuery query);
}
