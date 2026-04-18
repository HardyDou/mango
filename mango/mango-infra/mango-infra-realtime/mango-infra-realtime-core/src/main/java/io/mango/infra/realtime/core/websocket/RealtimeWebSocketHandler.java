package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import io.mango.infra.realtime.api.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.dispatcher.ProtocolRealtimeSender;
import io.mango.infra.realtime.core.inbound.NoopRealtimeInboundTransport;
import io.mango.infra.realtime.core.inbound.RealtimeInboundTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler implements ProtocolRealtimeSender {

    private final RealtimeSubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;
    private final RealtimeInboundTransport inboundTransport;
    private final int maxPayloadBytes;

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager, ObjectMapper objectMapper) {
        this(subscriptionManager, objectMapper, new NoopRealtimeInboundTransport(), 64 * 1024);
    }

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager,
                                    ObjectMapper objectMapper,
                                    RealtimeInboundTransport inboundTransport,
                                    int maxPayloadBytes) {
        this.subscriptionManager = subscriptionManager;
        this.objectMapper = objectMapper;
        this.inboundTransport = inboundTransport == null ? new NoopRealtimeInboundTransport() : inboundTransport;
        this.maxPayloadBytes = maxPayloadBytes <= 0 ? 64 * 1024 : maxPayloadBytes;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketRealtimeSession messageSession = new WebSocketRealtimeSession(session, objectMapper);
        subscriptionManager.subscribe(messageSession);
        log.info("WebSocket session established for tenant: {}, total connections: {}",
                messageSession.tenantId(), subscriptionManager.countByTenant(messageSession.tenantId()));
        messageSession.send(RealtimeMessage.of("connected", "WebSocket connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketRealtimeSession realtimeSession = new WebSocketRealtimeSession(session, objectMapper);
        try {
            if (message.getPayloadLength() > maxPayloadBytes) {
                realtimeSession.send(RealtimeMessage.of("error", "Realtime inbound message too large"));
                return;
            }
            WebSocketInboundMessage data = objectMapper.readValue(message.getPayload(), WebSocketInboundMessage.class);
            String type = data.type();
            if ("ping".equals(type)) {
                realtimeSession.send(RealtimeMessage.of("pong", null));
                return;
            }
            inboundTransport.forward(new RealtimeInboundMessage(
                    data.id(),
                    data.type(),
                    data.content(),
                    realtimeSession.tenantId(),
                    realtimeSession.userId(),
                    realtimeSession.id(),
                    data.headers(),
                    null));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse WebSocket message: {}", e.getOriginalMessage());
            realtimeSession.send(RealtimeMessage.of("error", "Invalid message format"));
        } catch (Exception e) {
            log.warn("Failed to process WebSocket message: {}", e.getMessage());
            realtimeSession.send(RealtimeMessage.of("error", "Invalid message format"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        subscriptionManager.unsubscribe(session.getId());
        log.info("WebSocket session closed: {}, reason: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session: {}", session.getId(), exception);
        subscriptionManager.unsubscribe(session.getId());
    }

    @Override
    public String protocol() {
        return RealtimeProtocols.WEBSOCKET;
    }

    @Override
    public void sendToUser(Long userId, RealtimeMessage envelope) {
        subscriptionManager.findByUser(userId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToTenant(String tenantId, RealtimeMessage envelope) {
        subscriptionManager.findByTenant(tenantId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void broadcast(RealtimeMessage envelope) {
        subscriptionManager.findAll().stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }
}
