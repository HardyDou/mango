package io.mango.infra.realtime.core.dispatcher;

import io.mango.infra.realtime.api.RealtimeMessage;

/**
 * Internal protocol adapter contract used by the composite publisher.
 */
public interface ProtocolRealtimeSender {

    String protocol();

    void sendToUser(Long userId, RealtimeMessage envelope);

    void sendToTenant(String tenantId, RealtimeMessage envelope);

    void broadcast(RealtimeMessage envelope);
}
