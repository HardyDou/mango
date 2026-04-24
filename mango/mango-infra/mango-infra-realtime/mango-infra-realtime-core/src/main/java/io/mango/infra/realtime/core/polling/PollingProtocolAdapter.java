package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import lombok.RequiredArgsConstructor;

/**
 * Protocol adapter that stores messages for HTTP polling clients.
 */
@RequiredArgsConstructor
public class PollingProtocolAdapter implements RealtimeProtocolSender {

    private final InMemoryRealtimePollingService pollingService;

    @Override
    public String protocol() {
        return RealtimeProtocols.POLLING;
    }

    @Override
    public void sendToUser(Long userId, RealtimeOutboundMessage envelope) {
        pollingService.publishToUser(userId, envelope);
    }

    @Override
    public void sendToTenant(String tenantId, RealtimeOutboundMessage envelope) {
        pollingService.publishToTenant(tenantId, envelope);
    }

    @Override
    public void broadcast(RealtimeOutboundMessage envelope) {
        pollingService.broadcast(envelope);
    }
}
