package io.mango.resource.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTypesTest {

    @Test
    void commonResourceTypes_areExposedFromResourceApi() {
        assertThat(ResourceTypes.FRONTEND_APP_REGISTRY).isEqualTo("FRONTEND_APP_REGISTRY");
        assertThat(ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY)
                .isEqualTo("FRONTEND_MODULE_RUNTIME_STRATEGY");
        assertThat(ResourceTypes.AUTH_ROLE).isEqualTo("AUTH_ROLE");
        assertThat(ResourceTypes.AUTH_ROLE_DATA_SCOPE).isEqualTo("AUTH_ROLE_DATA_SCOPE");
        assertThat(ResourceTypes.AUTH_SUBJECT_ROLE).isEqualTo("AUTH_SUBJECT_ROLE");
        assertThat(ResourceTypes.ORG_UNIT).isEqualTo("ORG_UNIT");
        assertThat(ResourceTypes.ORG_POST).isEqualTo("ORG_POST");
        assertThat(ResourceTypes.ORG_MEMBER_BINDING).isEqualTo("ORG_MEMBER_BINDING");
        assertThat(ResourceTypes.IDENTITY_USER).isEqualTo("IDENTITY_USER");
    }
}
