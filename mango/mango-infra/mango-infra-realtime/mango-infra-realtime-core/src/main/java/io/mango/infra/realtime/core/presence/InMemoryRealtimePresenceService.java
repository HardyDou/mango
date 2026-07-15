package io.mango.infra.realtime.core.presence;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRealtimePresenceService implements IRealtimePresenceService {

    private final ConcurrentHashMap<String, RealtimePresence> presences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.Set<String>> groupSessionIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.Set<String>> sessionGroupKeys = new ConcurrentHashMap<>();

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
            java.util.Set<String> groupKeys = sessionGroupKeys.remove(sessionId);
            if (groupKeys != null) {
                groupKeys.forEach(groupKey -> removeGroupSession(groupKey, sessionId));
            }
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
        String resolvedTenantId = defaultTenantId(tenantId);
        return presences.values().stream()
                .filter(presence -> resolvedTenantId.equals(presence.tenantId()))
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findByClient(String tenantId, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return List.of();
        }
        String resolvedTenantId = defaultTenantId(tenantId);
        return presences.values().stream()
                .filter(presence -> resolvedTenantId.equals(presence.tenantId()))
                .filter(presence -> clientId.equals(presence.clientId()))
                .toList();
    }

    @Override
    public Collection<RealtimePresence> findByConnection(String connectionId) {
        if (connectionId == null) {
            return List.of();
        }
        RealtimePresence presence = presences.get(connectionId);
        if (presence == null) {
            return List.of();
        }
        return List.of(presence);
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
        String key = groupKey(tenantId, groupId);
        groupSessionIds.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionGroupKeys.computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet()).add(key);
    }

    @Override
    public void leaveGroup(String sessionId, String tenantId, String groupId) {
        if (sessionId == null || groupId == null || groupId.isBlank()) {
            return;
        }
        String key = groupKey(tenantId, groupId);
        removeGroupSession(key, sessionId);
        java.util.Set<String> groupKeys = sessionGroupKeys.get(sessionId);
        if (groupKeys != null) {
            groupKeys.remove(key);
            if (groupKeys.isEmpty()) {
                sessionGroupKeys.remove(sessionId, groupKeys);
            }
        }

    }

    private void removeGroupSession(String key, String sessionId) {
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
        String resolvedTenantId = defaultTenantId(tenantId);
        return resolvedTenantId + ":" + groupId.trim();
    }

    private String defaultTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId;
    }
}
