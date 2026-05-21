package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.core.inbound.forward.RealtimeControlMessageHandler;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
import io.mango.infra.realtime.core.sse.SseRealtimeSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "实时 SSE", description = "实时消息 SSE 连接与上行接口")
public class SseRealtimeController {

    private final SseProtocolAdapter sseProtocolAdapter;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;
    private final RealtimeConnectionTicketService ticketService;
    private final RealtimeSubscriptionManager subscriptionManager;

    @GetMapping(value = "${mango.infra.realtime.sse.endpoint:/realtime/transports/sse}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 实时连接", description = "登录接口。按租户和用户建立 SSE 实时消息连接")
    public SseEmitter connect(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantIdHeader,
            @Parameter(description = "用户ID请求头")
            @RequestHeader(value = RealtimeHeaders.USER_ID, required = false) Long userIdHeader,
            @Parameter(description = "客户端ID请求头")
            @RequestHeader(value = RealtimeHeaders.CLIENT_ID, required = false) String clientIdHeader,
            @Parameter(description = "租户ID")
            @RequestParam(value = "tenantId", required = false) String tenantIdParam,
            @Parameter(description = "用户ID")
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest request) {

        String tenantId = firstText(tenantIdHeader, attributeText(request, "tenantId"), tenantIdParam, "default");
        Long resolvedUserId = firstLong(userIdHeader, attributeText(request, "userId"), userId);
        String clientId = firstText(clientIdHeader, request.getParameter("clientId"));
        SseRealtimeSession session = sseProtocolAdapter.createSession(tenantId, resolvedUserId, clientId);
        SseEmitter emitter = session.emitter();
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(new RealtimeOutboundMessage(
                            null,
                            "1.0",
                            RealtimeEvent.of("system", "connection.connected"),
                            RealtimeSource.server(),
                            RealtimeContext.of(tenantId, resolvedUserId),
                            null,
                            Map.of(
                                    "connectionId", session.id(),
                                    "clientId", clientId == null ? "" : clientId),
                            RealtimePayload.message("SSE connected"),
                            null,
                            null,
                            null,
                            null,
                            null)));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event", e);
        }
        return emitter;
    }

    @GetMapping(value = "/realtime/transports/probe/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "探测 SSE 链路", description = "使用短期实时 ticket 检查 SSE 链路，不注册业务订阅")
    public SseEmitter probe(@RequestParam(value = "rtTicket", required = false) String rtTicket) {
        ticketService.resolve(rtTicket)
                .orElseThrow(() -> new IllegalArgumentException("Missing or expired realtime ticket"));
        SseEmitter emitter = new SseEmitter(3000L);
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(Map.of("type", "probe.ok", "protocol", "sse")));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping("${mango.infra.realtime.sse.inbound-endpoint:/realtime/messages/inbound/sse}")
    @Operation(summary = "发送 SSE 上行消息", description = "登录接口。通过 SSE 通道提交客户端上行消息")
    public RealtimeOutboundMessage inbound(
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = "TENANT-ID", required = false) String legacyTenantIdHeader,
            @RequestHeader(value = RealtimeHeaders.USER_ID, required = false) Long userIdHeader,
            @RequestHeader(value = RealtimeHeaders.CLIENT_ID, required = false) String clientIdHeader,
            @RequestParam(value = "tenantId", required = false) String tenantIdParam,
            @RequestParam(value = "userId", required = false) Long userIdParam,
            @RequestParam(value = "clientId", required = false) String clientIdParam,
            @RequestParam(value = "sessionId", required = false) String sessionIdParam,
            @RequestBody RealtimeInboundMessage message) {
        RealtimeInboundMessage enrichedMessage = enrichInboundMessage(
                message,
                firstText(tenantIdHeader, legacyTenantIdHeader, tenantIdParam),
                firstLong(userIdHeader, userIdParam),
                firstText(clientIdHeader, clientIdParam),
                sessionIdParam);
        RealtimeOutboundMessage controlAck = RealtimeControlMessageHandler.handle(subscriptionManager, enrichedMessage.sessionId(), enrichedMessage);
        return controlAck == null ? inboundForwarder.forward(enrichedMessage) : controlAck;
    }

    private RealtimeInboundMessage enrichInboundMessage(
            RealtimeInboundMessage message,
            String tenantId,
            Long userId,
            String clientId,
            String sessionId) {
        RealtimeContext context = new RealtimeContext(
                firstText(tenantId, message.context().tenantId()),
                userId == null ? message.context().userId() : userId,
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

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String attributeText(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    private Long firstLong(Object... values) {
        for (Object value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
