package io.mango.authorization.api;

/**
 * 根据授权查询贡献权限快照。
 */
public interface AuthorityContributor {

    default boolean supports(AuthorizationQuery query) {
        return true;
    }

    AuthorizationSnapshot contribute(AuthorizationQuery query);
}
