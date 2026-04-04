package io.mango.infra.sse.controller;

import io.mango.infra.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE controller for server-sent events push
 * <p>
 * Supports heartbeat and tenant isolation via TENANT-ID header.
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * Connect to SSE stream
     * <p>
     * Headers:
     * - Authorization: Bearer {token} - authentication token
     * - TENANT-ID: {tenantId} - tenant identifier for isolation
     *
     * @param authorization authorization header
     * @param tenantId      tenant identifier
     * @return SSE emitter stream
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "TENANT-ID", required = false) String tenantId) {

        // Validate authorization (simplified - in production, validate JWT token)
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("SSE connection rejected: missing or invalid Authorization header");
            return createErrorEmitter("Missing or invalid Authorization header");
        }

        // Default tenant ID if not provided
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("SSE connection established for tenant: {}", tenantId);

        SseEmitter emitter = sseService.createEmitter(tenantId);

        // Send initial connection success event
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("{\"type\":\"connected\",\"content\":\"SSE connected\"}"));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event", e);
        }

        return emitter;
    }

    /**
     * Create error emitter that immediately fails
     */
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
