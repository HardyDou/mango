package io.mango.infra.realtime.api;

/**
 * Class-level subscriber for inbound realtime messages.
 */
public interface RealtimeSubscriber {

    void onMessage(RealtimeInboundMessage message);
}
