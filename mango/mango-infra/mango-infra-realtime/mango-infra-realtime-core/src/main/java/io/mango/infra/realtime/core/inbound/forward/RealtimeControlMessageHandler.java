package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.api.dto.RealtimeTargetType;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;

public final class RealtimeControlMessageHandler {

    private RealtimeControlMessageHandler() {
    }

    public static RealtimeOutboundMessage handle(RealtimeSubscriptionManager subscriptionManager,
                                                 String sessionId,
                                                 RealtimeInboundMessage message) {
        if (subscriptionManager == null || sessionId == null || message == null) {
            return null;
        }
        if (!"system".equals(message.event().domain())) {
            return null;
        }
        RealtimeTarget target = message.resolvedTarget();
        if (target.type() != RealtimeTargetType.GROUP || target.id().isBlank()) {
            return null;
        }
        if ("subscription.subscribe".equals(message.event().name())) {
            subscriptionManager.subscribeGroup(sessionId, target.id());
            return RealtimeOutboundMessage.accepted(message, "已订阅群组“" + target.id() + "”");
        }
        if ("subscription.unsubscribe".equals(message.event().name())) {
            subscriptionManager.unsubscribeGroup(sessionId, target.id());
            return RealtimeOutboundMessage.accepted(message, "已取消订阅群组“" + target.id() + "”");
        }
        return null;
    }
}
