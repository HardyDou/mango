package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;

import java.util.function.Supplier;

public class ProtocolRealtimeInboundForwarder {

    private final IRealtimeInboundForwardService inboundForwardService;
    private final Supplier<IRealtimePublishService> publishServiceSupplier;
    private final RealtimeInboundTargetAuthorizer targetAuthorizer;

    public ProtocolRealtimeInboundForwarder(IRealtimeInboundForwardService inboundForwardService) {
        this(inboundForwardService, () -> null, message -> false);
    }

    public ProtocolRealtimeInboundForwarder(IRealtimeInboundForwardService inboundForwardService,
                                            Supplier<IRealtimePublishService> publishServiceSupplier) {
        this(inboundForwardService, publishServiceSupplier, message -> false);
    }

    public ProtocolRealtimeInboundForwarder(IRealtimeInboundForwardService inboundForwardService,
                                            Supplier<IRealtimePublishService> publishServiceSupplier,
                                            RealtimeInboundTargetAuthorizer targetAuthorizer) {
        this.inboundForwardService = defaultForwardService(inboundForwardService);
        this.publishServiceSupplier = defaultPublishServiceSupplier(publishServiceSupplier);
        this.targetAuthorizer = defaultTargetAuthorizer(targetAuthorizer);
    }

    public RealtimeOutboundMessage forward(RealtimeInboundMessage envelope) {
        inboundForwardService.forward(envelope);
        publishTargetMessage(envelope);
        RealtimeOutboundMessage ack = RealtimeOutboundMessage.accepted(envelope, "我收到你发送的消息“" + envelope.content() + "”");
        if (envelope.sessionId() == null || envelope.sessionId().isBlank()) {
            return new RealtimeOutboundMessage(
                    ack.id(),
                    ack.version(),
                    ack.event(),
                    ack.source(),
                    ack.context(),
                    envelope.target(),
                    ack.metadata(),
                    ack.payload(),
                    ack.ack(),
                    ack.sequence(),
                    ack.status(),
                    ack.timestamp(),
                    ack.stream());
        }
        return ack;
    }

    private void publishTargetMessage(RealtimeInboundMessage envelope) {
        if (envelope.target() == null || isInternalControlMessage(envelope) || !targetAuthorizer.canPublish(envelope)) {
            return;
        }
        IRealtimePublishService publishService = publishServiceSupplier.get();
        if (publishService == null) {
            return;
        }
        publishService.publish(new RealtimeOutboundMessage(
                envelope.id(),
                envelope.version(),
                envelope.event(),
                envelope.source(),
                envelope.context(),
                envelope.target(),
                envelope.metadata(),
                envelope.payload(),
                null,
                envelope.sequence(),
                null,
                envelope.timestamp(),
                envelope.stream()));
    }

    private boolean isInternalControlMessage(RealtimeInboundMessage envelope) {
        if (!"system".equals(envelope.event().domain())) {
            return false;
        }
        String name = envelope.event().name();
        return name != null && (name.startsWith("subscription.")
                || name.startsWith("heartbeat.")
                || name.startsWith("connection."));
    }

    private IRealtimeInboundForwardService defaultForwardService(IRealtimeInboundForwardService service) {
        if (service == null) {
            return RealtimeInboundForwardServices.noop();
        }
        return service;
    }

    private Supplier<IRealtimePublishService> defaultPublishServiceSupplier(
            Supplier<IRealtimePublishService> supplier) {
        if (supplier == null) {
            return () -> null;
        }
        return supplier;
    }

    private RealtimeInboundTargetAuthorizer defaultTargetAuthorizer(RealtimeInboundTargetAuthorizer authorizer) {
        if (authorizer == null) {
            return message -> false;
        }
        return authorizer;
    }
}
