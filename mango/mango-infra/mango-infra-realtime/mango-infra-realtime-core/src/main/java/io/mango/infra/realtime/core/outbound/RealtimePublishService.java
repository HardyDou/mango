package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.presence.RealtimePresence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * Publishes one logical message through every enabled protocol adapter.
 */
@Slf4j
@RequiredArgsConstructor
public class RealtimePublishService implements IRealtimePublishService {

    private final List<RealtimeProtocolSender> senders;
    private final IRealtimePresenceService presenceService;
    private final IRealtimeOutboundForwardService outboundForwardService;
    private final RealtimeNode localNode;

    public RealtimePublishService(List<RealtimeProtocolSender> senders) {
        this(senders, null, new NoopRealtimeOutboundForwardService(),
                new RealtimeNode("local", "application", "/", "/_realtime/messages/outbound"));
    }

    @Override
    public void publish(RealtimeOutboundMessage realtimeOutboundMessage) {
        publishLocal(realtimeOutboundMessage);
        if (presenceService == null) {
            return;
        }
        outboundForwardService.forward(remotePresences(realtimeOutboundMessage), realtimeOutboundMessage);
    }

    @Override
    public void publishLocal(RealtimeOutboundMessage realtimeOutboundMessage) {
        for (RealtimeProtocolSender sender : senders) {
            try {
                if (realtimeOutboundMessage.userId() != null) {
                    sender.sendToUser(realtimeOutboundMessage.userId(), realtimeOutboundMessage);
                } else if (realtimeOutboundMessage.tenantId() != null && !"default".equals(realtimeOutboundMessage.tenantId())) {
                    sender.sendToTenant(realtimeOutboundMessage.tenantId(), realtimeOutboundMessage);
                } else {
                    sender.broadcast(realtimeOutboundMessage);
                }
            } catch (Exception e) {
                log.warn("Failed to publish realtime message {} through {}", realtimeOutboundMessage.id(), sender.protocol(), e);
            }
        }
    }

    private Collection<RealtimePresence> remotePresences(RealtimeOutboundMessage message) {
        Collection<RealtimePresence> presences;
        if (message.userId() != null) {
            presences = presenceService.findByUser(message.userId());
        } else if (message.tenantId() != null && !"default".equals(message.tenantId())) {
            presences = presenceService.findByTenant(message.tenantId());
        } else {
            presences = presenceService.findAll();
        }
        return presences.stream()
                .filter(presence -> !localNode.isLocal(presence))
                .toList();
    }
}
