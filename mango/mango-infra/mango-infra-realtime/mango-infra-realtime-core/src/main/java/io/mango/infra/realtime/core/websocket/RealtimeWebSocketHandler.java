package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.inbound.forward.IRealtimeInboundForwardService;
import io.mango.infra.realtime.core.inbound.forward.RealtimeInboundForwardServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler implements RealtimeProtocolSender {

    private final RealtimeSubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;
    private final IRealtimeInboundForwardService inboundForwardService;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;
    private final int maxPayloadBytes;

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager, ObjectMapper objectMapper) {
        this(subscriptionManager, objectMapper, RealtimeInboundForwardServices.noop(), 64 * 1024);
    }

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager,
                                    ObjectMapper objectMapper,
                                    IRealtimeInboundForwardService inboundForwardService,
                                    int maxPayloadBytes) {
        this.subscriptionManager = subscriptionManager;
        this.objectMapper = objectMapper;
        this.inboundForwardService = inboundForwardService == null
                ? RealtimeInboundForwardServices.noop()
                : inboundForwardService;
        this.inboundForwarder = new ProtocolRealtimeInboundForwarder(this.inboundForwardService);
        this.maxPayloadBytes = maxPayloadBytes <= 0 ? 64 * 1024 : maxPayloadBytes;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketRealtimeSession messageSession = new WebSocketRealtimeSession(session, objectMapper);
        subscriptionManager.subscribe(messageSession);
        log.info("WebSocket session established for tenant: {}, total connections: {}",
                messageSession.tenantId(), subscriptionManager.countByTenant(messageSession.tenantId()));
        messageSession.send(RealtimeOutboundMessage.of("connected", "WebSocket connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketRealtimeSession realtimeSession = new WebSocketRealtimeSession(session, objectMapper);
        try {
            if (message.getPayloadLength() > maxPayloadBytes) {
                realtimeSession.send(RealtimeOutboundMessage.of("error", "Realtime inbound message too large"));
                return;
            }
            WebSocketInboundMessage data = objectMapper.readValue(message.getPayload(), WebSocketInboundMessage.class);
            String type = data.type();
            if ("ping".equals(type)) {
                realtimeSession.send(RealtimeOutboundMessage.of("pong", null));
                return;
            }
            inboundForwarder.forward(
                    data.id(),
                    data.type(),
                    data.content(),
                    realtimeSession.tenantId(),
                    realtimeSession.userId(),
                    realtimeSession.id(),
                    data.headers());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse WebSocket message: {}", e.getOriginalMessage());
            realtimeSession.send(RealtimeOutboundMessage.of("error", "Invalid message format"));
        } catch (Exception e) {
            log.warn("Failed to process WebSocket message: {}", e.getMessage());
            realtimeSession.send(RealtimeOutboundMessage.of("error", "Invalid message format"));
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
    public void sendToUser(Long userId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByUser(userId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToTenant(String tenantId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByTenant(tenantId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void broadcast(RealtimeOutboundMessage envelope) {
        subscriptionManager.findAll().stream()
                .filter(session -> protocol().equals(session.protocol()))
                .forEach(session -> session.send(envelope));
    }
}
