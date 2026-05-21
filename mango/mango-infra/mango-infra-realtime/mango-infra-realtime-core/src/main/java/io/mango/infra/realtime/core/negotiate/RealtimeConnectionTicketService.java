package io.mango.infra.realtime.core.negotiate;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RealtimeConnectionTicketService {

    private static final long DEFAULT_TTL_MILLIS = 60_000L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, RealtimeConnectionTicket> tickets = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long ttlMillis;

    public RealtimeConnectionTicketService() {
        this(Clock.systemUTC(), DEFAULT_TTL_MILLIS);
    }

    public RealtimeConnectionTicketService(Clock clock, long ttlMillis) {
        this.clock = clock;
        this.ttlMillis = ttlMillis <= 0 ? DEFAULT_TTL_MILLIS : ttlMillis;
    }

    public RealtimeConnectionTicket issue(String tenantId, Long userId, String clientId, Map<String, Object> profile) {
        cleanupExpired();
        String value = newTicketValue();
        long expiresAt = clock.millis() + ttlMillis;
        RealtimeConnectionTicket ticket = new RealtimeConnectionTicket(
                value,
                tenantId == null || tenantId.isBlank() ? "default" : tenantId,
                userId,
                clientId,
                profile == null ? Map.of() : Map.copyOf(profile),
                expiresAt);
        tickets.put(value, ticket);
        return ticket;
    }

    public Optional<RealtimeConnectionTicket> resolve(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        RealtimeConnectionTicket ticket = tickets.get(value);
        if (ticket == null) {
            return Optional.empty();
        }
        if (ticket.expired(clock.millis())) {
            tickets.remove(value);
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    private String newTicketValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanupExpired() {
        long now = clock.millis();
        tickets.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }
}
