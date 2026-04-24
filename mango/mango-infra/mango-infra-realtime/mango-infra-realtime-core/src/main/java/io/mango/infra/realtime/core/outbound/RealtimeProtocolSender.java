package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Internal protocol adapter contract used by the composite publisher.
 */
public interface RealtimeProtocolSender {

    String protocol();

    void sendToUser(Long userId, RealtimeOutboundMessage envelope);

    void sendToTenant(String tenantId, RealtimeOutboundMessage envelope);

    void broadcast(RealtimeOutboundMessage envelope);
}
