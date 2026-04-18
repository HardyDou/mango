package io.mango.infra.realtime.api;

import java.util.Collection;

/**
 * Tracks connected client sessions and tenant/user subscriptions.
 */
public interface RealtimeSubscriptionManager {

    void subscribe(RealtimeSession session);

    void unsubscribe(String sessionId);

    Collection<RealtimeSession> findByTenant(String tenantId);

    Collection<RealtimeSession> findByUser(Long userId);

    Collection<RealtimeSession> findAll();

    int countByTenant(String tenantId);
}
