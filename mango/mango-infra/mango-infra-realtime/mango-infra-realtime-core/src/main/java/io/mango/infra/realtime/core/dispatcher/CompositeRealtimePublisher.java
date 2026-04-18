package io.mango.infra.realtime.core.dispatcher;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Publishes one logical message through every enabled protocol adapter.
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeRealtimePublisher implements RealtimePublisher {

    private final List<ProtocolRealtimeSender> senders;

    @Override
    public void publish(RealtimeMessage envelope) {
        for (ProtocolRealtimeSender sender : senders) {
            try {
                if (envelope.userId() != null) {
                    sender.sendToUser(envelope.userId(), envelope);
                } else if (envelope.tenantId() != null && !"default".equals(envelope.tenantId())) {
                    sender.sendToTenant(envelope.tenantId(), envelope);
                } else {
                    sender.broadcast(envelope);
                }
            } catch (Exception e) {
                log.warn("Failed to publish realtime message {} through {}", envelope.id(), sender.protocol(), e);
            }
        }
    }
}
