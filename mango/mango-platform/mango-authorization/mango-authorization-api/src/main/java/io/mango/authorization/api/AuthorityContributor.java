package io.mango.authorization.api;

import io.mango.authorization.api.vo.AuthorizationSnapshotVO;

/**
 * 根据授权查询贡献权限快照。
 */
public interface AuthorityContributor {

    default boolean supports(AuthorizationQuery query) {
        return true;
    }

    AuthorizationSnapshotVO contribute(AuthorizationQuery query);
}
