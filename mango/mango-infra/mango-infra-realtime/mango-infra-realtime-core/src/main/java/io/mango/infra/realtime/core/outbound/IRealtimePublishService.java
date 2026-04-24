package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

public interface IRealtimePublishService {

    void publish(RealtimeOutboundMessage realtimeOutboundMessage);

    void publishLocal(RealtimeOutboundMessage realtimeOutboundMessage);
}
