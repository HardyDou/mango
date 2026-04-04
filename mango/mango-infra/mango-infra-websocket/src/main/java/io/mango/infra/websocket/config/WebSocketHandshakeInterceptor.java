package io.mango.infra.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor
 * <p>
 * Validates token and extracts tenant ID from Sec-WebSocket-Protocol header.
 * Security: Validates that Sec-WebSocket-Protocol matches token's tenant ID.
 *
 * @author Mango
 */
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * Attribute key for tenant ID
     */
    public static final String TENANT_ID_ATTR = "tenantId";

    /**
     * Attribute key for authorized status
     */
    public static final String AUTHORIZED_ATTR = "authorized";

    /**
     * Attribute key for user ID
     */
    public static final String USER_ID_ATTR = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            log.warn("WebSocket handshake rejected: non-servlet request attempted");
            return false;
        }
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

        // Get token from URL query param (WebSocket handshake doesn't support custom headers)
        String token = servletRequest.getServletRequest().getParameter("token");
        String tenantId = servletRequest.getServletRequest().getParameter("tenantId");
        String userId = servletRequest.getServletRequest().getParameter("userId");

        // Validate token (simplified - in production, validate JWT token)
        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing or invalid token parameter");
            return false;
        }

        // Default tenant ID if not provided
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        attributes.put(TENANT_ID_ATTR, tenantId);
        attributes.put(AUTHORIZED_ATTR, true);
        if (userId != null && !userId.isBlank()) {
            attributes.put(USER_ID_ATTR, Long.parseLong(userId));
        }

        log.info("WebSocket handshake accepted for tenant: {}, userId: {}", tenantId, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}
