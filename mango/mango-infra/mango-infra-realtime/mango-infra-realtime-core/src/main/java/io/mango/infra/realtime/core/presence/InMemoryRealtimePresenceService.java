package io.mango.infra.realtime.core.presence;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRealtimePresenceService implements IRealtimePresenceService {

    private final ConcurrentHashMap<String, RealtimePresence> presences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.Set<String>> groupSessionIds = new ConcurrentHashMap<>();

    @Override
    public void online(RealtimePresence presence) {
        if (presence == null || presence.sessionId() == null || presence.sessionId().isBlank()) {
            return;
        }
        presences.put(presence.sessionId(), presence);
    }

    @Override
    public void offline(String sessionId) {
        if (sessionId != null) {
            presences.remove(sessionId);
        }
    }

    @Override
    public Collection<RealtimePresence> findByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return presences.values().stream()
                .filter(presence -> userId.equals(presence.userId()))
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findByTenant(String tenantId) {
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        return presences.values().stream()
                .filter(presence -> resolvedTenantId.equals(presence.tenantId()))
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findByClient(String tenantId, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return List.of();
        }
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        return presences.values().stream()
                .filter(presence -> resolvedTenantId.equals(presence.tenantId()))
                .filter(presence -> clientId.equals(presence.clientId()))
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findByConnection(String connectionId) {
        RealtimePresence presence = connectionId == null ? null : presences.get(connectionId);
        return presence == null ? List.of() : List.of(presence);
    }

    @Override
    public Collection<RealtimePresence> findByGroup(String tenantId, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return List.of();
        }
        java.util.Set<String> sessionIds = groupSessionIds.get(groupKey(tenantId, groupId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(presences::get)
                .filter(presence -> presence != null)
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findAll() {
        return List.copyOf(presences.values());
    }

    @Override
    public void joinGroup(String sessionId, String tenantId, String groupId) {
        if (sessionId == null || sessionId.isBlank() || groupId == null || groupId.isBlank()) {
            return;
        }
        groupSessionIds.computeIfAbsent(groupKey(tenantId, groupId), key -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @Override
    public void leaveGroup(String sessionId, String tenantId, String groupId) {
        if (sessionId == null || groupId == null || groupId.isBlank()) {
            return;
        }
        String key = groupKey(tenantId, groupId);
        java.util.Set<String> sessionIds = groupSessionIds.get(key);
        if (sessionIds == null) {
            return;
        }
        sessionIds.remove(sessionId);
        if (sessionIds.isEmpty()) {
            groupSessionIds.remove(key, sessionIds);
        }
    }

    private String groupKey(String tenantId, String groupId) {
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        return resolvedTenantId + ":" + groupId.trim();
    }
}
