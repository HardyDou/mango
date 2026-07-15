package io.mango.infra.realtime.core.polling;

/**
 * Identifies a polling subscriber's membership in a tenant group.
 */
public record RealtimeGroupSubscriptionKey(
        String subscriberId,
        String tenantId,
        String groupId) {
}
