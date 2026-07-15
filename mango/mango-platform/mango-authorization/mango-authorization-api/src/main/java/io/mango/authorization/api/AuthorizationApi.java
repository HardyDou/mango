package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import jakarta.validation.Valid;

/**
 * 授权查询远程契约。
 */
public interface AuthorizationApi {

    R<AuthorizationSnapshotVO> loadUserAuthorization(@Valid LoadUserAuthorizationQuery query);
}
