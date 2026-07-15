package io.mango.authorization.api;

import io.mango.authorization.api.vo.AuthorizationSnapshotVO;

/**
 * 本地与远程安全集成都使用的授权提供者。
 */
public interface IAuthorizationProvider {

    AuthorizationSnapshotVO load(AuthorizationQuery query);
}
