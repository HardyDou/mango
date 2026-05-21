package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.core.session.RealtimeSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class WebSocketRealtimeSession implements RealtimeSession {

    private final WebSocketSession session;
    private final ObjectMapper objectMapper;

    @Override
    public String id() {
        return session.getId();
    }

    @Override
    public String protocol() {
        return RealtimeProtocols.WEBSOCKET;
    }

    @Override
    public String tenantId() {
        Object tenantId = session.getAttributes().get(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR);
        return tenantId == null ? "default" : tenantId.toString();
    }

    @Override
    public Long userId() {
        Object userId = session.getAttributes().get(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR);
        if (userId == null) {
            return null;
        }
        if (userId instanceof Long value) {
            return value;
        }
        try {
            return Long.parseLong(userId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String clientId() {
        Object clientId = session.getAttributes().get(RealtimeWebSocketHandshakeInterceptor.CLIENT_ID_ATTR);
        return clientId == null ? null : clientId.toString();
    }

    public Map<String, Object> profile() {
        Object profile = session.getAttributes().get(RealtimeWebSocketHandshakeInterceptor.PROFILE_ATTR);
        if (!(profile instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(key.toString(), value);
            }
        });
        return result;
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public void send(RealtimeOutboundMessage envelope) {
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send WebSocket message", e);
        }
    }
}
