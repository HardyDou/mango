package io.mango.infra.realtime.core.presence;

import java.util.Collection;

public interface IRealtimePresenceService {

    void online(RealtimePresence presence);

    void offline(String sessionId);

    Collection<RealtimePresence> findByUser(Long userId);

    Collection<RealtimePresence> findByTenant(String tenantId);

    Collection<RealtimePresence> findAll();
}
