package io.mango.infra.realtime.api.dto;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Protocol names used by realtime sessions and protocol adapters.
 */
@LocalCapabilityContract
public final class RealtimeProtocols {

    public static final String SSE = "SSE";

    public static final String WEBSOCKET = "WEBSOCKET";

    public static final String POLLING = "POLLING";

    private RealtimeProtocols() {
    }
}
