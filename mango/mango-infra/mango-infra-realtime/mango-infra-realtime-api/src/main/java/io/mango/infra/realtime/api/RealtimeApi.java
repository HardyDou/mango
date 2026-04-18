package io.mango.infra.realtime.api;

/**
 * Realtime publishing contract used by business modules.
 */
public interface RealtimeApi {

    void publish(RealtimeMessage message);

    default void publishToUser(Long userId, String type, String content) {
        publish(RealtimeMessage.toUser(userId, type, content));
    }

    default void publishToTenant(String tenantId, String type, String content) {
        publish(RealtimeMessage.toTenant(tenantId, type, content));
    }

    default void broadcast(String type, String content) {
        publish(RealtimeMessage.of(type, content));
    }
}
