package io.mango.infra.realtime.core.sse;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SseProtocolAdapter implements RealtimeProtocolSender {

    public static final long DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final RealtimeSubscriptionManager subscriptionManager;
    private final long timeoutMillis;

    public SseProtocolAdapter(RealtimeSubscriptionManager subscriptionManager) {
        this(subscriptionManager, DEFAULT_TIMEOUT_MILLIS);
    }

    public SseProtocolAdapter(RealtimeSubscriptionManager subscriptionManager, long timeoutMillis) {
        this.subscriptionManager = subscriptionManager;
        this.timeoutMillis = timeoutMillis <= 0 ? DEFAULT_TIMEOUT_MILLIS : timeoutMillis;
    }

    public SseRealtimeSession createSession(String tenantId, Long userId, String clientId) {
        SseEmitter emitter = createSseEmitter();
        String sessionId = UUID.randomUUID().toString();
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        AtomicBoolean closed = new AtomicBoolean(false);

        Runnable closeCallback = () -> {
            if (closed.compareAndSet(false, true)) {
                subscriptionManager.unsubscribe(sessionId);
                log.info("SSE session removed for tenant: {}, remaining connections: {}",
                        resolvedTenantId, subscriptionManager.countByTenant(resolvedTenantId));
            }
        };

        SseRealtimeSession session = new SseRealtimeSession(sessionId, resolvedTenantId, userId, clientId, emitter, closeCallback);
        subscriptionManager.subscribe(session);

        log.info("SSE session created for tenant: {}, total connections: {}",
                resolvedTenantId, subscriptionManager.countByTenant(resolvedTenantId));
        return session;
    }

    public SseEmitter createEmitter(String tenantId, Long userId, String clientId) {
        return createSession(tenantId, userId, clientId).emitter();
    }

    protected SseEmitter createSseEmitter() {
        return new SseEmitter(timeoutMillis);
    }

    @Override
    public String protocol() {
        return RealtimeProtocols.SSE;
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
