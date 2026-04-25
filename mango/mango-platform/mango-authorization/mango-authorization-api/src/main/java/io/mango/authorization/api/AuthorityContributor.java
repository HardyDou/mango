package io.mango.authorization.api;

/**
 * Contributes authorities for an authorization query.
 */
public interface AuthorityContributor {

    default boolean supports(AuthorizationQuery query) {
        return true;
    }

    AuthorizationSnapshot contribute(AuthorizationQuery query);
}
