package io.mango.authorization.api;

import io.mango.resource.api.ResourceTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationResourceTypesTest {

    @Test
    void frontendRuntimeResourceTypes_delegateToResourceApiConstants() {
        assertThat(AuthorizationResourceTypes.FRONTEND_APP_REGISTRY)
                .isEqualTo(ResourceTypes.FRONTEND_APP_REGISTRY);
        assertThat(AuthorizationResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY)
                .isEqualTo(ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY);
    }
}
