package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.core.web.RealtimeRequestIdentityResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class RealtimeWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TENANT_ID_ATTR = "tenantId";
    public static final String AUTHORIZED_ATTR = "authorized";
    public static final String USER_ID_ATTR = "userId";
    public static final String CLIENT_ID_ATTR = "clientId";
    public static final String PROFILE_ATTR = "profile";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake rejected: non-servlet request attempted");
            return false;
        }

        String tenantId = RealtimeRequestIdentityResolver.resolveTenantId(
                servletRequest.getServletRequest(),
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.TENANT_ID),
                servletRequest.getServletRequest().getParameter("tenantId"));
        Long userId = RealtimeRequestIdentityResolver.resolveUserId(
                servletRequest.getServletRequest(),
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.USER_ID),
                servletRequest.getServletRequest().getParameter("userId"));
        String clientId = firstText(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.CLIENT_ID),
                servletRequest.getServletRequest().getParameter("clientId"));

        attributes.put(TENANT_ID_ATTR, defaultTenantId(tenantId));
        attributes.put(AUTHORIZED_ATTR, true);
        attributes.put(PROFILE_ATTR, Map.of());
        if (userId != null) {
            attributes.put(USER_ID_ATTR, userId);
        }
        if (clientId != null) {
            attributes.put(CLIENT_ID_ATTR, clientId);
        }
        return true;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String defaultTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake.
    }
}
