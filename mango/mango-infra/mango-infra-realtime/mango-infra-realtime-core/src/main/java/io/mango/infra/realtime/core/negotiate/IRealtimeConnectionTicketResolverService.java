package io.mango.infra.realtime.core.negotiate;

import java.util.Optional;

/**
 * Resolves short-lived realtime connection tickets.
 */
public interface IRealtimeConnectionTicketResolverService {

    Optional<RealtimeConnectionTicket> resolve(String value);
}
