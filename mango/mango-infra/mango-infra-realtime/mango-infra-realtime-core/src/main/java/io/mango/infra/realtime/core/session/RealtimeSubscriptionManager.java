package io.mango.infra.realtime.core.session;

import java.util.Collection;

/**
 * Tracks connected client sessions and tenant/user subscriptions.
 */
public interface RealtimeSubscriptionManager {

    void subscribe(RealtimeSession session);

    void unsubscribe(String sessionId);

    void subscribeGroup(String sessionId, String groupId);

    void unsubscribeGroup(String sessionId, String groupId);

    Collection<RealtimeSession> findByTenant(String tenantId);

    Collection<RealtimeSession> findByUser(Long userId);

    Collection<RealtimeSession> findByClient(String tenantId, String clientId);

    Collection<RealtimeSession> findByConnection(String connectionId);

    Collection<RealtimeSession> findByGroup(String tenantId, String groupId);

    Collection<RealtimeSession> findAll();

    int countByTenant(String tenantId);
}
