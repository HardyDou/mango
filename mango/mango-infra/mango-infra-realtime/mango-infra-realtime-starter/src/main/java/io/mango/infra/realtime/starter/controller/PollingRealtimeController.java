package io.mango.infra.realtime.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePollingQuery;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.api.dto.RealtimeTargetType;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.negotiate.IRealtimeConnectionTicketResolverService;
import io.mango.infra.realtime.core.web.RealtimeRequestIdentityResolver;
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
import java.util.Map;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Realtime services are injected Spring singleton collaborators"))
@Tag(name = "实时轮询", description = "实时消息轮询传输接口")
public class PollingRealtimeController {

    private final InMemoryRealtimePollingService pollingService;
    private final int defaultMaxSize;
    private final int maxSize;
    private final long defaultTimeoutMillis;
    private final long maxTimeoutMillis;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;
    private final IRealtimeConnectionTicketResolverService ticketService;

    @GetMapping("${mango.infra.realtime.polling.endpoint:/realtime/transports/polling}")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "轮询实时消息")
    @Operation(summary = "轮询实时消息", description = "登录接口。按用户轮询实时消息，支持长轮询超时和最大消息数")
    public DeferredResult<List<RealtimeOutboundMessage>> poll(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantIdHeader,
            @Parameter(description = "用户ID请求头")
            @RequestHeader(value = RealtimeHeaders.USER_ID, required = false) Long userIdHeader,
            @Parameter(description = "客户端ID请求头")
            @RequestHeader(value = RealtimeHeaders.CLIENT_ID, required = false) String clientIdHeader,
            @Parameter(description = "租户ID")
            @RequestParam(value = "tenantId", required = false) String tenantIdParam,
            @ParameterObject RealtimePollingQuery query) {
        Long userId = RealtimeRequestIdentityResolver.resolveUserId(userIdHeader, query.getUserId());
        String tenantId = RealtimeRequestIdentityResolver.resolveTenantId(tenantIdHeader, tenantIdParam);
        String subscriberId = resolvePollingSubscriber(tenantId, userId, clientIdHeader);
        int effectiveMaxSize = normalizeMaxSize(query.getMaxSize());
        long effectiveTimeoutMillis = normalizeTimeoutMillis(query.getTimeoutMillis());
        pollingService.register(subscriberId, tenantId, userId, clientIdHeader);
        return pollingService.pollAsync(
                subscriberId,
                tenantId,
                effectiveMaxSize,
                effectiveTimeoutMillis);
    }

    private int normalizeMaxSize(Integer requestedMaxSize) {
        int value = defaultIfNonPositive(requestedMaxSize, defaultMaxSize);
        return Math.min(value, maxSize);
    }

    private long normalizeTimeoutMillis(Long requestedTimeoutMillis) {
        long value = defaultIfNegative(requestedTimeoutMillis, defaultTimeoutMillis);
        return Math.min(value, maxTimeoutMillis);
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @PostMapping("${mango.infra.realtime.polling.inbound-endpoint:/realtime/messages/inbound/polling}")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "发送轮询上行消息")
    @Operation(summary = "发送轮询上行消息", description = "登录接口。通过轮询通道提交客户端上行消息")
    public RealtimeOutboundMessage inbound(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = "TENANT-ID", required = false) String legacyTenantIdHeader,
            @RequestHeader(value = RealtimeHeaders.USER_ID, required = false) Long userIdHeader,
            @Parameter(description = "客户端ID请求头")
            @RequestHeader(value = RealtimeHeaders.CLIENT_ID, required = false) String clientIdHeader,
            @RequestParam(value = "tenantId", required = false) String tenantIdParam,
            @RequestParam(value = "userId", required = false) Long userIdParam,
            @RequestParam(value = "clientId", required = false) String clientIdParam,
            @RequestParam(value = "sessionId", required = false) String sessionIdParam,
            @Valid @RequestBody RealtimeInboundMessage message) {
        String tenantId = RealtimeRequestIdentityResolver.resolveTenantId(
                tenantIdHeader, legacyTenantIdHeader, tenantIdParam, message.context().tenantId());
        Long userId = RealtimeRequestIdentityResolver.resolveUserId(
                userIdHeader, userIdParam, message.context().userId());
        String clientId = firstText(clientIdHeader, clientIdParam, message.source().clientId());
        String subscriberId = resolvePollingSubscriber(tenantId, userId, clientId);
        RealtimeInboundMessage enrichedMessage = enrichInboundMessage(
                message,
                tenantId,
                userId,
                clientId,
                firstText(sessionIdParam, subscriberId));
        if ("system".equals(enrichedMessage.event().domain())
                && "subscription.subscribe".equals(enrichedMessage.event().name())
                && enrichedMessage.resolvedTarget().type() == RealtimeTargetType.GROUP) {
            pollingService.subscribeGroup(subscriberId, enrichedMessage.context().tenantId(), enrichedMessage.resolvedTarget().id());
            return RealtimeOutboundMessage.accepted(enrichedMessage, "已订阅群组“" + enrichedMessage.resolvedTarget().id() + "”");
        }
        if ("system".equals(enrichedMessage.event().domain())
                && "subscription.unsubscribe".equals(enrichedMessage.event().name())
                && enrichedMessage.resolvedTarget().type() == RealtimeTargetType.GROUP) {
            pollingService.unsubscribeGroup(subscriberId, enrichedMessage.context().tenantId(), enrichedMessage.resolvedTarget().id());
            return RealtimeOutboundMessage.accepted(enrichedMessage, "已取消订阅群组“" + enrichedMessage.resolvedTarget().id() + "”");
        }
        return inboundForwarder.forward(enrichedMessage);
    }

    private RealtimeInboundMessage enrichInboundMessage(
            RealtimeInboundMessage message,
            String tenantId,
            Long userId,
            String clientId,
            String sessionId) {
        RealtimeContext context = new RealtimeContext(
                firstText(tenantId, message.context().tenantId()),
                resolvedUserId(userId, message.context().userId()),
                message.context().traceId(),
                message.context().requestId());
        RealtimeSource source = new RealtimeSource(
                message.source().platform(),
                firstText(clientId, message.source().clientId()),
                firstText(sessionId, message.source().sessionId()));
        return new RealtimeInboundMessage(
                message.id(),
                message.version(),
                message.event(),
                source,
                context,
                message.target(),
                message.metadata(),
                message.payload(),
                message.ack(),
                message.sequence(),
                message.timestamp(),
                message.stream());
    }

    private String resolvePollingSubscriber(String tenantId, Long userId, String clientId) {
        if (clientId != null && !clientId.isBlank()) {
            return InMemoryRealtimePollingService.clientSubscriberId(tenantId, clientId);
        }
        return InMemoryRealtimePollingService.userSubscriberId(tenantId, userId);
    }

    private int defaultIfNonPositive(Integer candidate, int defaultValue) {
        if (candidate == null || candidate <= 0) {
            return defaultValue;
        }
        return candidate;
    }

    private long defaultIfNegative(Long candidate, long defaultValue) {
        if (candidate == null || candidate < 0) {
            return defaultValue;
        }
        return candidate;
    }

    private Long resolvedUserId(Long preferred, Long fallback) {
        if (preferred == null) {
            return fallback;
        }
        return preferred;
    }

    @GetMapping("/realtime/transports/probe/polling")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "探测轮询链路")
    @Operation(summary = "探测轮询链路", description = "检查 HTTP 轮询链路，不注册业务订阅")
    public Map<String, String> probe() {
        String rtTicket = RealtimeRequestIdentityResolver.currentRequestParameter("rtTicket");
        if (rtTicket != null && !rtTicket.isBlank()) {
            ticketService.resolve(rtTicket)
                    .orElseThrow(() -> new IllegalArgumentException("Missing or expired realtime ticket"));
        }
        return Map.of("type", "probe.ok", "protocol", "polling");
    }
}
