package io.mango.infra.realtime.api;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Realtime publishing contract used by business modules.
 */
public interface RealtimeApi {

    void publish(RealtimeOutboundMessage realtimeOutboundMessage);

    default void publishToUser(Long userId, String type, String content) {
        publish(RealtimeOutboundMessage.toUser(userId, type, content));
    }

    default void publishToTenant(String tenantId, String type, String content) {
        publish(RealtimeOutboundMessage.toTenant(tenantId, type, content));
    }

    default void broadcast(String type, String content) {
        publish(RealtimeOutboundMessage.of(type, content));
    }
}
