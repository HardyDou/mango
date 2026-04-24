package io.mango.infra.realtime.core.session;

import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.presence.RealtimePresence;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local subscription registry shared by protocol adapters.
 */
public class InMemoryRealtimeSubscriptionManager implements RealtimeSubscriptionManager {

    private final ConcurrentHashMap<String, RealtimeSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tenantSessionIds = new ConcurrentHashMap<>();
    private final IRealtimePresenceService presenceService;
    private final RealtimeNode localNode;

    public InMemoryRealtimeSubscriptionManager(IRealtimePresenceService presenceService, RealtimeNode localNode) {
        this.presenceService = Objects.requireNonNull(presenceService, "presenceService must not be null");
        this.localNode = Objects.requireNonNull(localNode, "localNode must not be null");
    }

    @Override
    public void subscribe(RealtimeSession session) {
        if (session == null || session.id() == null || session.id().isBlank()) {
            return;
        }
        RealtimeSession previous = sessions.put(session.id(), session);
        removeTenantIndex(previous);
        tenantSessionIds.computeIfAbsent(normalizeTenantId(session.tenantId()), key -> ConcurrentHashMap.newKeySet())
                .add(session.id());
        presenceService.online(RealtimePresence.of(
                session.id(),
                session.tenantId(),
                session.userId(),
                session.protocol(),
                localNode));
    }

    @Override
    public void unsubscribe(String sessionId) {
        if (sessionId != null) {
            removeTenantIndex(sessions.remove(sessionId));
            presenceService.offline(sessionId);
        }
    }

    @Override
    public Collection<RealtimeSession> findByTenant(String tenantId) {
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        Set<String> sessionIds = tenantSessionIds.get(resolvedTenantId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(session -> session != null)
                .filter(RealtimeSession::isOpen)
                .filter(session -> resolvedTenantId.equals(session.tenantId()))
                .toList();
    }

    @Override
    public Collection<RealtimeSession> findByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return sessions.values().stream()
                .filter(RealtimeSession::isOpen)
                .filter(session -> userId.equals(session.userId()))
                .toList();
    }

    @Override
    public Collection<RealtimeSession> findAll() {
        return sessions.values().stream()
                .filter(RealtimeSession::isOpen)
                .toList();
    }

    @Override
    public int countByTenant(String tenantId) {
        String resolvedTenantId = normalizeTenantId(tenantId);
        Set<String> sessionIds = tenantSessionIds.get(resolvedTenantId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }
        return (int) sessionIds.stream()
                .map(sessions::get)
                .filter(session -> session != null)
                .filter(RealtimeSession::isOpen)
                .filter(session -> resolvedTenantId.equals(session.tenantId()))
                .count();
    }

    private void removeTenantIndex(RealtimeSession session) {
        if (session == null) {
            return;
        }
        String tenantId = normalizeTenantId(session.tenantId());
        Set<String> sessionIds = tenantSessionIds.get(tenantId);
        if (sessionIds == null) {
            return;
        }
        sessionIds.remove(session.id());
        if (sessionIds.isEmpty()) {
            tenantSessionIds.remove(tenantId, sessionIds);
        }
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }
}
