package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.api.RealtimeHeaders;
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

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake rejected: non-servlet request attempted");
            return false;
        }

        String authorization = servletRequest.getServletRequest().getHeader(RealtimeHeaders.AUTHORIZATION);
        String token = firstText(authorization, servletRequest.getServletRequest().getParameter("token"));
        String tenantId = firstText(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.TENANT_ID),
                servletRequest.getServletRequest().getParameter("tenantId"));
        String userId = servletRequest.getServletRequest().getParameter("userId");

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing Authorization header or token parameter");
            return false;
        }

        attributes.put(TENANT_ID_ATTR, tenantId == null || tenantId.isBlank() ? "default" : tenantId);
        attributes.put(AUTHORIZED_ATTR, true);
        if (userId != null && !userId.isBlank()) {
            Long resolvedUserId = parseUserId(userId);
            if (resolvedUserId == null) {
                log.warn("WebSocket handshake rejected: invalid userId parameter");
                return false;
            }
            attributes.put(USER_ID_ATTR, resolvedUserId);
        }
        return true;
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake.
    }
}
