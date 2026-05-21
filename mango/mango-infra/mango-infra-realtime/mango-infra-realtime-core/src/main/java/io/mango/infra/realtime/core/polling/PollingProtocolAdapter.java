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
    public boolean acceptsUnregisteredTargets() {
        return true;
    }

    @Override
    public void sendToUser(Long userId, RealtimeOutboundMessage envelope) {
        pollingService.publishToUser(userId, envelope);
    }

    @Override
    public void sendToClient(String tenantId, String clientId, RealtimeOutboundMessage envelope) {
        pollingService.publishToClient(tenantId, clientId, envelope);
    }

    @Override
    public void sendToConnection(String connectionId, RealtimeOutboundMessage envelope) {
        pollingService.publishToConnection(connectionId, envelope);
    }

    @Override
    public void sendToGroup(String tenantId, String groupId, RealtimeOutboundMessage envelope) {
        pollingService.publishToGroup(tenantId, groupId, envelope);
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
