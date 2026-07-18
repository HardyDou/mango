package io.mango.infra.realtime.core.presence;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Collection;

@LocalCapabilityContract
public interface IRealtimePresenceService {

    void online(RealtimePresence presence);

    void offline(String sessionId);

    Collection<RealtimePresence> findByUser(Long userId);

    Collection<RealtimePresence> findByTenant(String tenantId);

    Collection<RealtimePresence> findByClient(String tenantId, String clientId);

    Collection<RealtimePresence> findByConnection(String connectionId);

    Collection<RealtimePresence> findByGroup(String tenantId, String groupId);

    Collection<RealtimePresence> findAll();

    default void joinGroup(String sessionId, String tenantId, String groupId) {
    }

    default void leaveGroup(String sessionId, String tenantId, String groupId) {
    }
}
