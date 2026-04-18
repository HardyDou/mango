package io.mango.infra.realtime.core.sse;

import io.mango.infra.realtime.api.RealtimeHeaders;
import io.mango.infra.realtime.api.RealtimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SseRealtimeController {

    private final SseProtocolAdapter sseProtocolAdapter;

    @GetMapping(value = "${mango.infra.realtime.sse.endpoint:/realtime/subscribe}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestHeader(value = RealtimeHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestParam(value = "userId", required = false) Long userId) {

        // Full token validation belongs to gateway/security; realtime only rejects malformed protocol entry.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("SSE connection rejected: missing or invalid Authorization header");
            return createErrorEmitter("Missing or invalid Authorization header");
        }

        SseEmitter emitter = sseProtocolAdapter.createEmitter(tenantId, userId);
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(RealtimeMessage.of("connected", "SSE connected")));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event", e);
        }
        return emitter;
    }

    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"type\":\"error\",\"message\":\"" + errorMessage + "\"}"));
        } catch (Exception e) {
            log.warn("Failed to send error event", e);
        }
        emitter.complete();
        return emitter;
    }
}
