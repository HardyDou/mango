package io.mango.ai.core.controller;

import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.core.service.ChatService;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI 对话", description = "AI 对话与流式响应接口")
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
    @Operation(summary = "发起 AI 流式对话", description = "受保护接口。提交对话内容并通过 SSE 返回流式响应")
    public SseEmitter chat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "AI 对话请求")
            @Valid @RequestBody ChatRequest chatRequest,
            @Parameter(description = "访问令牌，格式为 Bearer <accessToken>")
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "租户ID请求头")
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
