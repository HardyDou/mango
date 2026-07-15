package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "ObjectMapper and realtime services are injected singleton collaborators")
public class RealtimeWebSocketHandler extends TextWebSocketHandler implements RealtimeProtocolSender {

    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 64 * 1024;

    private final RealtimeSubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;
    private final IRealtimeInboundForwardService inboundForwardService;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;
    private final int maxPayloadBytes;

    public RealtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager, ObjectMapper objectMapper) {
        this(subscriptionManager, objectMapper, RealtimeInboundForwardServices.noop(), DEFAULT_MAX_PAYLOAD_BYTES);
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
        this.inboundForwardService = defaultForwardService(inboundForwardService);
        this.inboundForwarder = defaultForwarder(inboundForwarder, this.inboundForwardService);
        this.maxPayloadBytes = defaultMaxPayloadBytes(maxPayloadBytes);
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
                        "clientId", emptyIfNull(messageSession.clientId())),
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
        if (controlAck != null) {
            return controlAck;
        }
        return inboundForwarder.forward(data);
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

    private IRealtimeInboundForwardService defaultForwardService(IRealtimeInboundForwardService service) {
        if (service == null) {
            return RealtimeInboundForwardServices.noop();
        }
        return service;
    }

    private ProtocolRealtimeInboundForwarder defaultForwarder(ProtocolRealtimeInboundForwarder forwarder,
                                                               IRealtimeInboundForwardService service) {
        if (forwarder == null) {
            return new ProtocolRealtimeInboundForwarder(service);
        }
        return forwarder;
    }

    private int defaultMaxPayloadBytes(int candidate) {
        if (candidate <= 0) {
            return DEFAULT_MAX_PAYLOAD_BYTES;
        }
        return candidate;
    }

    private String emptyIfNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
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
