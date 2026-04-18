package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import io.mango.infra.realtime.core.dispatcher.ProtocolRealtimeSender;
import lombok.RequiredArgsConstructor;

/**
 * Protocol adapter that stores messages for HTTP polling clients.
 */
@RequiredArgsConstructor
public class PollingProtocolAdapter implements ProtocolRealtimeSender {

    private final InMemoryRealtimePollingService pollingService;

    @Override
    public String protocol() {
        return RealtimeProtocols.POLLING;
    }

    @Override
    public void sendToUser(Long userId, RealtimeMessage envelope) {
        pollingService.publishToUser(userId, envelope);
    }

    @Override
    public void sendToTenant(String tenantId, RealtimeMessage envelope) {
        pollingService.publishToTenant(tenantId, envelope);
    }

    @Override
    public void broadcast(RealtimeMessage envelope) {
        pollingService.broadcast(envelope);
    }
}
