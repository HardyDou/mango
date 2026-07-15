package io.mango.infra.realtime.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeRequestIdentityResolverTest {

    @Test
    void authenticatedAttributesOverrideClientControlledFallbacks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("tenantId", "trusted-tenant");
        request.setAttribute("userId", 1001L);

        assertThat(RealtimeRequestIdentityResolver.resolveTenantId(request, "spoofed-tenant"))
                .isEqualTo("trusted-tenant");
        assertThat(RealtimeRequestIdentityResolver.resolveUserId(request, 9999L))
                .isEqualTo(1001L);
    }

    @Test
    void gatewayFallbacksRemainCompatibleWithoutAuthenticatedAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(RealtimeRequestIdentityResolver.resolveTenantId(request, " gateway-tenant "))
                .isEqualTo("gateway-tenant");
        assertThat(RealtimeRequestIdentityResolver.resolveUserId(request, "2002"))
                .isEqualTo(2002L);
    }
}
