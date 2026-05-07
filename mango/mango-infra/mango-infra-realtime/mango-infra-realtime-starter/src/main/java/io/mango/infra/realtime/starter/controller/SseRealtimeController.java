package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "实时 SSE", description = "实时消息 SSE 连接与上行接口")
public class SseRealtimeController {

    private final SseProtocolAdapter sseProtocolAdapter;
    private final ProtocolRealtimeInboundForwarder inboundForwarder;

    @GetMapping(value = "${mango.infra.realtime.sse.endpoint:/realtime/transports/sse}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestParam(value = "userId", required = false) Long userId) {

        SseEmitter emitter = sseProtocolAdapter.createEmitter(tenantId, userId);
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(RealtimeOutboundMessage.of("connected", "SSE connected")));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event", e);
        }
        return emitter;
    }

    @PostMapping("${mango.infra.realtime.sse.inbound-endpoint:/realtime/messages/inbound/sse}")
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
