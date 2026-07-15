package io.mango.authorization.core.service;

import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;

/** 授权快照查询服务。 */
public interface IAuthorizationQueryService {

    AuthorizationSnapshotVO loadUserAuthorization(LoadUserAuthorizationQuery query);
}
