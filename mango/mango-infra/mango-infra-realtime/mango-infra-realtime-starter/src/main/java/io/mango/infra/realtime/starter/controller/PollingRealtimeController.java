package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePollingQuery;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "实时轮询", description = "实时消息轮询传输接口")
public class PollingRealtimeController {

    private final InMemoryRealtimePollingService pollingService;
    private final int defaultMaxSize;
    private final int maxSize;
    private final long defaultTimeoutMillis;
    private final long maxTimeoutMillis;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;

    @GetMapping("${mango.infra.realtime.polling.endpoint:/realtime/transports/polling}")
    @Operation(summary = "轮询实时消息", description = "登录接口。按用户轮询实时消息，支持长轮询超时和最大消息数")
    public DeferredResult<List<RealtimeOutboundMessage>> poll(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @ParameterObject RealtimePollingQuery query) {
        Long userId = query.getUserId();
        String subscriberId = InMemoryRealtimePollingService.userSubscriberId(userId);
        int effectiveMaxSize = normalizeMaxSize(query.getMaxSize());
        long effectiveTimeoutMillis = normalizeTimeoutMillis(query.getTimeoutMillis());
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
    @Operation(summary = "发送轮询上行消息", description = "登录接口。通过轮询通道提交客户端上行消息")
    public RealtimeOutboundMessage inbound(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestBody RealtimeInboundMessage message) {
        return inboundForwarder.forward(
                message.id(),
                message.type(),
                message.content(),
                tenantId != null ? tenantId : message.tenantId(),
                message.userId(),
                message.sessionId(),
                message.headers());
    }
}
