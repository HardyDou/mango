package io.mango.authorization.api;

import io.mango.resource.api.ResourceTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationResourceTypesTest {

    @Test
    void authorizationResourceTypes_delegateToResourceApiConstants() {
        assertThat(AuthorizationResourceTypes.FRONTEND_APP_REGISTRY)
                .isEqualTo(ResourceTypes.FRONTEND_APP_REGISTRY);
        assertThat(AuthorizationResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY)
                .isEqualTo(ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY);
        assertThat(AuthorizationResourceTypes.AUTH_ROLE).isEqualTo(ResourceTypes.AUTH_ROLE);
        assertThat(AuthorizationResourceTypes.AUTH_ROLE_DATA_SCOPE)
                .isEqualTo(ResourceTypes.AUTH_ROLE_DATA_SCOPE);
        assertThat(AuthorizationResourceTypes.AUTH_SUBJECT_ROLE)
                .isEqualTo(ResourceTypes.AUTH_SUBJECT_ROLE);
    }
}
