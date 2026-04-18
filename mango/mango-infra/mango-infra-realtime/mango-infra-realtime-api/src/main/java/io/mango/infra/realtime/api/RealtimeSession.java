package io.mango.infra.realtime.api;

/**
 * Protocol-neutral client connection session.
 */
public interface RealtimeSession {

    String id();

    String protocol();

    String tenantId();

    Long userId();

    boolean isOpen();

    void send(RealtimeMessage envelope);
}
