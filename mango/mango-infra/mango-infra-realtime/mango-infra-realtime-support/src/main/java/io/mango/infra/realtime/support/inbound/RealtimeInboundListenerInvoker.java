package io.mango.infra.realtime.support.inbound;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

public interface RealtimeInboundListenerInvoker {

    String[] types();

    int order();

    String description();

    void invoke(RealtimeInboundMessage message);
}
