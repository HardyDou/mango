package io.mango.authorization.api;

import io.mango.resource.api.ResourceTypes;

/**
 * Authorization module resource types consumed through Mango Resource Registry.
 */
public final class AuthorizationResourceTypes {

    public static final String FRONTEND_APP_REGISTRY = ResourceTypes.FRONTEND_APP_REGISTRY;
    public static final String FRONTEND_MODULE_RUNTIME_STRATEGY = ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY;

    private AuthorizationResourceTypes() {
    }
}
