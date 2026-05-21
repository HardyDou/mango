package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

public interface IRealtimeReliablePublishService {

    void publish(RealtimeOutboundMessage message);
}
