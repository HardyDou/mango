package io.mango.infra.realtime.core.negotiate;

import java.util.Map;

public record RealtimeConnectionTicket(
        String value,
        String tenantId,
        Long userId,
        String clientId,
        Map<String, Object> profile,
        long expiresAt) {

    public boolean expired(long now) {
        return now >= expiresAt;
    }
}
