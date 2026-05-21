package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
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

        String tenantId = firstText(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.TENANT_ID),
                attributeText(servletRequest, "tenantId"),
                servletRequest.getServletRequest().getParameter("tenantId"),
                "default");
        Long userId = firstLong(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.USER_ID),
                attributeText(servletRequest, "userId"),
                servletRequest.getServletRequest().getParameter("userId"));
        String clientId = firstText(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.CLIENT_ID),
                servletRequest.getServletRequest().getParameter("clientId"));

        attributes.put(TENANT_ID_ATTR, tenantId == null || tenantId.isBlank() ? "default" : tenantId);
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

    private String attributeText(ServletServerHttpRequest request, String name) {
        Object value = request.getServletRequest().getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    private Long firstLong(Object... values) {
        for (Object value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake.
    }
}
