package io.mango.authorization.api;

/**
 * Authorization HTTP contract.
 */
public interface AuthorizationApi {

    AuthorizationSnapshot loadUserAuthorization(Long subjectId, String tenantId, String systemCode);
}
