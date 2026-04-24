package io.mango.infra.realtime.e2e.support;

import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.presence.InMemoryRealtimePresenceService;

public final class SharedRealtimePresence {

    private static IRealtimePresenceService presenceService = new InMemoryRealtimePresenceService();

    private SharedRealtimePresence() {
    }

    public static synchronized void reset() {
        presenceService = new InMemoryRealtimePresenceService();
    }

    public static synchronized IRealtimePresenceService get() {
        return presenceService;
    }
}
