package io.mango.infra.realtime.api;

/**
 * Protocol names used by realtime sessions and protocol adapters.
 */
public final class RealtimeProtocols {

    public static final String SSE = "SSE";

    public static final String WEBSOCKET = "WEBSOCKET";

    public static final String POLLING = "POLLING";

    private RealtimeProtocols() {
    }
}
