package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Internal protocol adapter contract used by the composite publisher.
 */
public interface RealtimeProtocolSender {

    String protocol();

    default boolean acceptsUnregisteredTargets() {
        return false;
    }

    void sendToUser(Long userId, RealtimeOutboundMessage envelope);

    void sendToClient(String tenantId, String clientId, RealtimeOutboundMessage envelope);

    void sendToConnection(String connectionId, RealtimeOutboundMessage envelope);

    void sendToGroup(String tenantId, String groupId, RealtimeOutboundMessage envelope);

    void sendToTenant(String tenantId, RealtimeOutboundMessage envelope);

    void broadcast(RealtimeOutboundMessage envelope);
}
