package io.mango.authorization.api;

/**
 * Authorization provider used by local and remote security integrations.
 */
public interface IAuthorizationProvider {

    AuthorizationSnapshot load(AuthorizationQuery query);
}
