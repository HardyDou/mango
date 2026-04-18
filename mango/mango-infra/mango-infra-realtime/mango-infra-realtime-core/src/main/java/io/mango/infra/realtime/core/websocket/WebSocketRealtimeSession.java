package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import io.mango.infra.realtime.api.RealtimeSession;
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
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public void send(RealtimeMessage envelope) {
        try {
            String payload = objectMapper.writeValueAsString(toPayload(envelope));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send WebSocket message", e);
        }
    }

    private Map<String, Object> toPayload(RealtimeMessage envelope) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", envelope.id());
        payload.put("type", envelope.type());
        payload.put("content", envelope.content());
        payload.put("tenantId", envelope.tenantId());
        payload.put("userId", envelope.userId());
        payload.put("headers", envelope.headers());
        payload.put("createdAt", envelope.createdAt() == null ? null : envelope.createdAt().toString());
        return payload;
    }
}
