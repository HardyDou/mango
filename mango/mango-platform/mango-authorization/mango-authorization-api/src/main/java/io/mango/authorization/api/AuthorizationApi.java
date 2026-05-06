package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;

/**
 * 授权查询远程契约。
 */
public interface AuthorizationApi {

    R<AuthorizationSnapshot> loadUserAuthorization(LoadUserAuthorizationQuery query);
}
