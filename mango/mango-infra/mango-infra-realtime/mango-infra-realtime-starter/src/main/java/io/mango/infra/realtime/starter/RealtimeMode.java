package io.mango.infra.realtime.starter;

/**
 * Runtime mode for realtime protocol auto-configuration.
 */
public enum RealtimeMode {

    /**
     * Enables protocols according to each nested protocol switch.
     */
    AUTO,

    /**
     * Enables only SSE protocol beans.
     */
    SSE,

    /**
     * Enables only WebSocket protocol beans.
     */
    WEBSOCKET,

    /**
     * Enables only HTTP polling protocol beans.
     */
    POLLING
}
