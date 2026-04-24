package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PollingRealtimeController {

    private final InMemoryRealtimePollingService pollingService;
    private final int defaultMaxSize;
    private final int maxSize;
    private final long defaultTimeoutMillis;
    private final long maxTimeoutMillis;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;

    @GetMapping("${mango.infra.realtime.polling.endpoint:/realtime/transports/polling}")
    public DeferredResult<List<RealtimeOutboundMessage>> poll(
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "maxSize", required = false) Integer requestedMaxSize,
            @RequestParam(value = "timeoutMillis", required = false) Long requestedTimeoutMillis) {
        String subscriberId = InMemoryRealtimePollingService.userSubscriberId(userId);
        int effectiveMaxSize = normalizeMaxSize(requestedMaxSize);
        long effectiveTimeoutMillis = normalizeTimeoutMillis(requestedTimeoutMillis);
        return pollingService.pollAsync(subscriberId, normalizeTenantId(tenantId), effectiveMaxSize, effectiveTimeoutMillis);
    }

    private int normalizeMaxSize(Integer requestedMaxSize) {
        int value = requestedMaxSize == null || requestedMaxSize <= 0 ? defaultMaxSize : requestedMaxSize;
        return Math.min(value, maxSize);
    }

    private long normalizeTimeoutMillis(Long requestedTimeoutMillis) {
        long value = requestedTimeoutMillis == null || requestedTimeoutMillis < 0
                ? defaultTimeoutMillis
                : requestedTimeoutMillis;
        return Math.min(value, maxTimeoutMillis);
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    @PostMapping("${mango.infra.realtime.polling.inbound-endpoint:/realtime/messages/inbound/polling}")
    public RealtimeOutboundMessage inbound(
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestBody RealtimeInboundMessage message) {
        return inboundForwarder.forward(
                message.id(),
                message.type(),
                message.content(),
                tenantId != null ? tenantId : message.tenantId(),
                userId != null ? userId : message.userId(),
                sessionId != null ? sessionId : message.sessionId(),
                message.headers());
    }
}
