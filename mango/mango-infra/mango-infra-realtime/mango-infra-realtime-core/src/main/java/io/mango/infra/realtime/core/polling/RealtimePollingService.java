package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

import java.util.List;

/**
 * Local polling queue access used by the polling protocol adapter.
 */
public interface RealtimePollingService {

    void append(String subscriberId, RealtimeOutboundMessage envelope);

    List<RealtimeOutboundMessage> poll(String subscriberId, int maxSize);
}
