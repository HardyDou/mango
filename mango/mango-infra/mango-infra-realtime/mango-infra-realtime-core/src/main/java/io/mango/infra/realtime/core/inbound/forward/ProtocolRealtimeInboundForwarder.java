package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

import java.util.Map;

public class ProtocolRealtimeInboundForwarder {

    private final IRealtimeInboundForwardService inboundForwardService;

    public ProtocolRealtimeInboundForwarder(IRealtimeInboundForwardService inboundForwardService) {
        this.inboundForwardService = inboundForwardService;
    }

    public RealtimeOutboundMessage forward(String id,
                                           String type,
                                           String content,
                                           String tenantId,
                                           Long userId,
                                           String sessionId,
                                           Map<String, Object> headers) {
        inboundForwardService.forward(new RealtimeInboundMessage(
                id,
                type,
                content,
                normalizeTenantId(tenantId),
                userId,
                sessionId,
                headers,
                null));
        return RealtimeOutboundMessage.of("accepted", "inbound accepted");
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }
}
