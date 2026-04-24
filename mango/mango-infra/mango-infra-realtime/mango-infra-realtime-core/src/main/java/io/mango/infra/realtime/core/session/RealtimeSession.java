package io.mango.infra.realtime.core.session;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Protocol-neutral client connection session.
 */
public interface RealtimeSession {

    String id();

    String protocol();

    String tenantId();

    Long userId();

    boolean isOpen();

    void send(RealtimeOutboundMessage envelope);
}
