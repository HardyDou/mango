package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.api.dto.RealtimeTargetType;
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
        if (presenceService == null) {
            publishLocal(realtimeOutboundMessage);
            return;
        }
        boolean hasLocalPresences = hasLocalPresences(realtimeOutboundMessage);
        if (hasLocalPresences || realtimeOutboundMessage.resolvedTarget().type() == RealtimeTargetType.BROADCAST) {
            publishLocal(realtimeOutboundMessage);
        } else {
            publishLocalToUnregisteredTargetSenders(realtimeOutboundMessage);
        }
        Collection<RealtimePresence> remotePresences = remotePresences(realtimeOutboundMessage);
        if (!remotePresences.isEmpty() && outboundForwardService.isNoop()) {
            throw new IllegalStateException("Realtime outbound forwarding is required for remote presences");
        }
        outboundForwardService.forward(remotePresences, realtimeOutboundMessage);
    }

    @Override
    public void publishLocal(RealtimeOutboundMessage realtimeOutboundMessage) {
        for (RealtimeProtocolSender sender : senders) {
            try {
                publishLocal(sender, realtimeOutboundMessage);
            } catch (Exception e) {
                log.warn("Failed to publish realtime message {} through {}", realtimeOutboundMessage.id(), sender.protocol(), e);
            }
        }
    }

    private void publishLocalToUnregisteredTargetSenders(RealtimeOutboundMessage realtimeOutboundMessage) {
        for (RealtimeProtocolSender sender : senders) {
            if (!sender.acceptsUnregisteredTargets()) {
                continue;
            }
            try {
                publishLocal(sender, realtimeOutboundMessage);
            } catch (Exception e) {
                log.warn("Failed to publish realtime message {} through {}", realtimeOutboundMessage.id(), sender.protocol(), e);
            }
        }
    }

    private void publishLocal(RealtimeProtocolSender sender, RealtimeOutboundMessage message) {
        RealtimeTarget target = message.resolvedTarget();
        if (target.type() == RealtimeTargetType.USER) {
            sender.sendToUser(parseLong(target.id()), message);
            return;
        }
        if (target.type() == RealtimeTargetType.CLIENT) {
            sender.sendToClient(message.tenantId(), target.id(), message);
            return;
        }
        if (target.type() == RealtimeTargetType.CONNECTION) {
            sender.sendToConnection(target.id(), message);
            return;
        }
        if (target.type() == RealtimeTargetType.GROUP) {
            sender.sendToGroup(message.tenantId(), target.id(), message);
            return;
        }
        if (target.type() == RealtimeTargetType.TENANT) {
            sender.sendToTenant(target.id().isBlank() ? message.tenantId() : target.id(), message);
            return;
        }
        sender.broadcast(message);
    }

    private Collection<RealtimePresence> remotePresences(RealtimeOutboundMessage message) {
        RealtimeTarget target = message.resolvedTarget();
        Collection<RealtimePresence> presences = switch (target.type()) {
            case USER -> presenceService.findByUser(parseLong(target.id()));
            case CLIENT -> presenceService.findByClient(message.tenantId(), target.id());
            case CONNECTION -> presenceService.findByConnection(target.id());
            case GROUP -> presenceService.findByGroup(message.tenantId(), target.id());
            case TENANT -> presenceService.findByTenant(target.id().isBlank() ? message.tenantId() : target.id());
            case BROADCAST -> presenceService.findAll();
        };
        return presences.stream()
                .filter(presence -> !localNode.isLocal(presence))
                .toList();
    }

    private boolean hasLocalPresences(RealtimeOutboundMessage message) {
        RealtimeTarget target = message.resolvedTarget();
        Collection<RealtimePresence> presences = switch (target.type()) {
            case USER -> presenceService.findByUser(parseLong(target.id()));
            case CLIENT -> presenceService.findByClient(message.tenantId(), target.id());
            case CONNECTION -> presenceService.findByConnection(target.id());
            case GROUP -> presenceService.findByGroup(message.tenantId(), target.id());
            case TENANT -> presenceService.findByTenant(target.id().isBlank() ? message.tenantId() : target.id());
            case BROADCAST -> presenceService.findAll();
        };
        return presences.stream().anyMatch(localNode::isLocal);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
