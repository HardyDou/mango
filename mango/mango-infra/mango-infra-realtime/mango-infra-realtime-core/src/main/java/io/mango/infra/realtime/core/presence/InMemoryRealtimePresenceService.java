package io.mango.infra.realtime.core.presence;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRealtimePresenceService implements IRealtimePresenceService {

    private final ConcurrentHashMap<String, RealtimePresence> presences = new ConcurrentHashMap<>();

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
    public Collection<RealtimePresence> findAll() {
        return List.copyOf(presences.values());
    }
}
