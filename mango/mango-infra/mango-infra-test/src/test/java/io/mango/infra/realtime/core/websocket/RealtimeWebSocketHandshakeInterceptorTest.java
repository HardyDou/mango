package io.mango.infra.realtime.core.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeWebSocketHandshakeInterceptorTest {

    private final RealtimeWebSocketHandshakeInterceptor interceptor = new RealtimeWebSocketHandshakeInterceptor();

    @Test
    void beforeHandshake_headerTenantOverridesQueryTenant() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/realtime/ws");
        servletRequest.addHeader("Authorization", "Bearer test-token");
        servletRequest.addHeader("TENANT-ID", "tenant-header");
        servletRequest.setParameter("tenantId", "tenant-query");
        servletRequest.setParameter("userId", "1001");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), null, null, attributes);

        assertTrue(accepted);
        assertEquals("tenant-header", attributes.get(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR));
        assertEquals(1001L, attributes.get(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR));
    }

    @Test
    void beforeHandshake_queryTenantRemainsCompatibilityFallback() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/realtime/ws");
        servletRequest.setParameter("token", "test-token");
        servletRequest.setParameter("tenantId", "tenant-query");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), null, null, attributes);

        assertTrue(accepted);
        assertEquals("tenant-query", attributes.get(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR));
    }

    @Test
    void beforeHandshake_missingTokenRejectsConnection() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/realtime/ws");

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), null, null, new HashMap<>());

        assertFalse(accepted);
    }

    @Test
    void beforeHandshake_invalidUserIdRejectsConnection() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/realtime/ws");
        servletRequest.addHeader("Authorization", "Bearer test-token");
        servletRequest.setParameter("userId", "not-a-number");

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), null, null, new HashMap<>());

        assertFalse(accepted);
    }
}
