package io.mango.ai.core.controller;

import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.core.service.ChatService;
import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话控制器。
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
     * SSE 流式对话接口。
     *
     * @param chatRequest 对话请求
     * @param authorization 认证请求头
     * @param tenantId 租户 ID
     * @return SSE 推送流
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @Valid @RequestBody ChatRequest chatRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = MangoContextHeaders.TENANT_ID, required = false) String tenantId) {

        // 校验认证请求头。
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Chat request rejected: missing or invalid Authorization header");
            return createErrorEmitter("Missing or invalid Authorization header");
        }

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = MangoContextHolder.tenantId();
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("Chat request received for tenant: {}, sessionId: {}", tenantId, chatRequest.getSessionId());

        return chatService.chat(chatRequest, tenantId);
    }

    /**
     * 创建错误事件流。
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
