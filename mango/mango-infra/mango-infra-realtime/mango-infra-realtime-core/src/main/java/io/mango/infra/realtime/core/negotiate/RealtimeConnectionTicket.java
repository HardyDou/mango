package io.mango.infra.realtime.core.negotiate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Profile is defensively copied to an immutable map by the compact constructor")
public record RealtimeConnectionTicket(
        String value,
        String tenantId,
        Long userId,
        String clientId,
        Map<String, Object> profile,
        long expiresAt) {

    public RealtimeConnectionTicket {
        profile = copyProfile(profile);
    }

    private static Map<String, Object> copyProfile(Map<String, Object> source) {
        if (source == null) {
            return Map.of();
        }
        return Map.copyOf(source);
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }
}
