package io.mango.resource.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTypesTest {

    @Test
    void frontendRuntimeResourceTypes_areExposedFromResourceApi() {
        assertThat(ResourceTypes.FRONTEND_APP_REGISTRY).isEqualTo("FRONTEND_APP_REGISTRY");
        assertThat(ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY)
                .isEqualTo("FRONTEND_MODULE_RUNTIME_STRATEGY");
    }
}
