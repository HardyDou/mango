package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.inbound.forward.IRealtimeInboundForwardService;
import io.mango.infra.realtime.core.inbound.forward.RealtimeControlMessageHandler;
import io.mango.infra.realtime.core.inbound.forward.RealtimeInboundForwardServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

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
        this(subscriptionManager,
                objectMapper,
                inboundForwardService,
                new ProtocolRealtimeInboundForwarder(inboundForwardService),
                maxPayloadBytes);
    }

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager,
                                    ObjectMapper objectMapper,
                                    IRealtimeInboundForwardService inboundForwardService,
                                    ProtocolRealtimeInboundForwarder inboundForwarder,
                                    int maxPayloadBytes) {
        this.subscriptionManager = subscriptionManager;
        this.objectMapper = objectMapper;
        this.inboundForwardService = inboundForwardService == null
                ? RealtimeInboundForwardServices.noop()
                : inboundForwardService;
        this.inboundForwarder = inboundForwarder == null
                ? new ProtocolRealtimeInboundForwarder(this.inboundForwardService)
                : inboundForwarder;
        this.maxPayloadBytes = maxPayloadBytes <= 0 ? 64 * 1024 : maxPayloadBytes;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketRealtimeSession messageSession = new WebSocketRealtimeSession(session, objectMapper);
        subscriptionManager.subscribe(messageSession);
        log.info("WebSocket session established for tenant: {}, total connections: {}",
                messageSession.tenantId(), subscriptionManager.countByTenant(messageSession.tenantId()));
        messageSession.send(new RealtimeOutboundMessage(
                null,
                "1.0",
                RealtimeEvent.of("system", "connection.connected"),
                RealtimeSource.server(),
                RealtimeContext.of(messageSession.tenantId(), messageSession.userId()),
                null,
                Map.of(
                        "profile", messageSession.profile(),
                        "connectionId", messageSession.id(),
                        "clientId", messageSession.clientId() == null ? "" : messageSession.clientId()),
                RealtimePayload.message("WebSocket connected"),
                null,
                null,
                null,
                null,
                null));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketRealtimeSession realtimeSession = new WebSocketRealtimeSession(session, objectMapper);
        try {
            if (message.getPayloadLength() > maxPayloadBytes) {
                realtimeSession.send(RealtimeOutboundMessage.of("error", "Realtime inbound message too large"));
                return;
            }
            if (isPingFrame(message.getPayload())) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                return;
            }
            RealtimeInboundMessage data = objectMapper.readValue(message.getPayload(), RealtimeInboundMessage.class);
            RealtimeOutboundMessage ack = forwardOrHandleControl(realtimeSession, enrichInboundMessage(realtimeSession, data));
            realtimeSession.send(ack);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse WebSocket message: {}", e.getOriginalMessage());
            realtimeSession.send(RealtimeOutboundMessage.of("error", "Invalid message format"));
        } catch (Exception e) {
            log.warn("Failed to process WebSocket message: {}", e.getMessage());
            realtimeSession.send(RealtimeOutboundMessage.of("error", "Invalid message format"));
        }
    }

    private RealtimeOutboundMessage forwardOrHandleControl(WebSocketRealtimeSession session, RealtimeInboundMessage data) {
        RealtimeOutboundMessage controlAck = RealtimeControlMessageHandler.handle(subscriptionManager, session.id(), data);
        return controlAck == null ? inboundForwarder.forward(data) : controlAck;
    }

    private RealtimeInboundMessage enrichInboundMessage(WebSocketRealtimeSession session, RealtimeInboundMessage message) {
        RealtimeContext context = new RealtimeContext(
                session.tenantId(),
                session.userId(),
                message.context().traceId(),
                message.context().requestId());
        RealtimeSource source = new RealtimeSource(
                message.source().platform(),
                firstText(session.clientId(), message.source().clientId()),
                session.id());
        return new RealtimeInboundMessage(
                message.id(),
                message.version(),
                message.event(),
                source,
                context,
                message.target(),
                message.metadata(),
                message.payload(),
                message.ack(),
                message.sequence(),
                message.timestamp(),
                message.stream());
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

    private boolean isPingFrame(String payload) {
        return "{\"type\":\"ping\"}".equals(payload) || "ping".equals(payload);
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
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToClient(String tenantId, String clientId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByClient(tenantId, clientId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToConnection(String connectionId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByConnection(connectionId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToGroup(String tenantId, String groupId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByGroup(tenantId, groupId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void sendToTenant(String tenantId, RealtimeOutboundMessage envelope) {
        subscriptionManager.findByTenant(tenantId).stream()
                .filter(session -> protocol().equals(session.protocol()))
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    @Override
    public void broadcast(RealtimeOutboundMessage envelope) {
        subscriptionManager.findAll().stream()
                .filter(session -> protocol().equals(session.protocol()))
                .filter(session -> !isSourceSession(session, envelope))
                .forEach(session -> session.send(envelope));
    }

    private boolean isSourceSession(io.mango.infra.realtime.core.session.RealtimeSession session,
                                    RealtimeOutboundMessage envelope) {
        return envelope.source() != null
                && envelope.source().sessionId() != null
                && envelope.source().sessionId().equals(session.id());
    }
}
