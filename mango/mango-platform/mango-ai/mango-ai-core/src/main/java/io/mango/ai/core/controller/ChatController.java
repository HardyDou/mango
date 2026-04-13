package io.mango.ai.core.controller;

import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.core.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Chat controller for AI conversation
 * <p>
 * Supports SSE streaming responses with tenant isolation.
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Chat endpoint with SSE streaming
     * <p>
     * Headers:
     * - Authorization: Bearer {token} - authentication token
     * - TENANT-ID: {tenantId} - tenant identifier for isolation
     *
     * Request body:
     * - message: the chat message (required, max 2000 chars)
     * - sessionId: optional session identifier for conversation context
     * - enableThinking: optional, default true
     *
     * @param chatRequest chat request
     * @param authorization authorization header
     * @param tenantId tenant identifier
     * @return SSE emitter stream
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @Valid @RequestBody ChatRequest chatRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "TENANT-ID", required = false) String tenantId) {

        // Validate authorization
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Chat request rejected: missing or invalid Authorization header");
            return createErrorEmitter("Missing or invalid Authorization header");
        }

        // Security check: TENANT-ID must match token's tenant ID
        // In production, extract tenant ID from JWT token and compare
        // For now, use provided tenant ID or default
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("Chat request received for tenant: {}, sessionId: {}", tenantId, chatRequest.getSessionId());

        return chatService.chat(chatRequest, tenantId);
    }

    /**
     * Create error emitter
     */
    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"" + errorMessage + "\"}"));
        } catch (Exception e) {
            log.warn("Failed to send error event", e);
        }
        emitter.complete();
        return emitter;
    }
}
