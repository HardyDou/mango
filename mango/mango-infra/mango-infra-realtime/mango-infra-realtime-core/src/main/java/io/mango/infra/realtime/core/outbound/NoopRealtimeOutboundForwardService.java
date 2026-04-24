package io.mango.infra.realtime.core.outbound;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.presence.RealtimePresence;

import java.util.Collection;

public class NoopRealtimeOutboundForwardService implements IRealtimeOutboundForwardService {

    @Override
    public void forward(Collection<RealtimePresence> presences, RealtimeOutboundMessage message) {
    }
}
