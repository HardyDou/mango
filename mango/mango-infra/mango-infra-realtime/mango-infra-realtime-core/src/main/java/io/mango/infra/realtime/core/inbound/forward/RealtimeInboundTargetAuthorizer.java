package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

@FunctionalInterface
public interface RealtimeInboundTargetAuthorizer {

    boolean canPublish(RealtimeInboundMessage message);
}
