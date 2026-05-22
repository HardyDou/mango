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
    private final ConcurrentHashMap<String, Set<String>> clientSessionIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groupSessionIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionGroupKeys = new ConcurrentHashMap<>();
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
        removeClientIndex(previous);
        tenantSessionIds.computeIfAbsent(normalizeTenantId(session.tenantId()), key -> ConcurrentHashMap.newKeySet())
                .add(session.id());
        if (session.clientId() != null && !session.clientId().isBlank()) {
            clientSessionIds.computeIfAbsent(clientKey(session.tenantId(), session.clientId()), key -> ConcurrentHashMap.newKeySet())
                    .add(session.id());
        }
        presenceService.online(RealtimePresence.of(
                session.id(),
                session.tenantId(),
                session.userId(),
                session.clientId(),
                session.protocol(),
                localNode));
    }

    @Override
    public void unsubscribe(String sessionId) {
        if (sessionId != null) {
            RealtimeSession session = sessions.remove(sessionId);
            removeTenantIndex(session);
            removeClientIndex(session);
            removeGroupIndexes(sessionId);
            presenceService.offline(sessionId);
        }
    }

    @Override
    public void subscribeGroup(String sessionId, String groupId) {
        RealtimeSession session = sessions.get(sessionId);
        String normalizedGroupId = normalizeGroupId(groupId);
        if (session == null || normalizedGroupId == null) {
            return;
        }
        String key = groupKey(session.tenantId(), normalizedGroupId);
        groupSessionIds.computeIfAbsent(key, value -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionGroupKeys.computeIfAbsent(sessionId, value -> ConcurrentHashMap.newKeySet()).add(key);
        presenceService.joinGroup(sessionId, normalizeTenantId(session.tenantId()), normalizedGroupId);
    }

    @Override
    public void unsubscribeGroup(String sessionId, String groupId) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (sessionId == null || normalizedGroupId == null) {
            return;
        }
        RealtimeSession session = sessions.get(sessionId);
        String key = groupKey(session == null ? null : session.tenantId(), normalizedGroupId);
        removeFromGroup(sessionId, key);
        presenceService.leaveGroup(sessionId, normalizeTenantId(session == null ? null : session.tenantId()), normalizedGroupId);
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
    public Collection<RealtimeSession> findByClient(String tenantId, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return List.of();
        }
        String key = clientKey(tenantId, clientId);
        Set<String> sessionIds = clientSessionIds.get(key);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return openSessions(sessionIds);
    }

    @Override
    public Collection<RealtimeSession> findByConnection(String connectionId) {
        RealtimeSession session = connectionId == null ? null : sessions.get(connectionId);
        if (session == null || !session.isOpen()) {
            return List.of();
        }
        return List.of(session);
    }

    @Override
    public Collection<RealtimeSession> findByGroup(String tenantId, String groupId) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId == null) {
            return List.of();
        }
        Set<String> sessionIds = groupSessionIds.get(groupKey(tenantId, normalizedGroupId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return openSessions(sessionIds);
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

    private void removeClientIndex(RealtimeSession session) {
        if (session == null || session.clientId() == null || session.clientId().isBlank()) {
            return;
        }
        String key = clientKey(session.tenantId(), session.clientId());
        Set<String> sessionIds = clientSessionIds.get(key);
        if (sessionIds == null) {
            return;
        }
        sessionIds.remove(session.id());
        if (sessionIds.isEmpty()) {
            clientSessionIds.remove(key, sessionIds);
        }
    }

    private void removeGroupIndexes(String sessionId) {
        Set<String> keys = sessionGroupKeys.remove(sessionId);
        if (keys == null) {
            return;
        }
        keys.forEach(key -> removeFromGroup(sessionId, key));
    }

    private void removeFromGroup(String sessionId, String key) {
        Set<String> sessionIds = groupSessionIds.get(key);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                groupSessionIds.remove(key, sessionIds);
            }
        }
        Set<String> keys = sessionGroupKeys.get(sessionId);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                sessionGroupKeys.remove(sessionId, keys);
            }
        }
    }

    private Collection<RealtimeSession> openSessions(Set<String> sessionIds) {
        return sessionIds.stream()
                .map(sessions::get)
                .filter(session -> session != null)
                .filter(RealtimeSession::isOpen)
                .toList();
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    private String normalizeGroupId(String groupId) {
        return groupId == null || groupId.isBlank() ? null : groupId.trim();
    }

    private String clientKey(String tenantId, String clientId) {
        return normalizeTenantId(tenantId) + ":" + clientId.trim();
    }

    private String groupKey(String tenantId, String groupId) {
        return normalizeTenantId(tenantId) + ":" + groupId.trim();
    }
}
