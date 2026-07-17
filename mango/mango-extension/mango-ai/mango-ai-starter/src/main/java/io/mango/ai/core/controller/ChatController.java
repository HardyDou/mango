package io.mango.ai.core.controller;

import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.core.service.IChatService;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * AI 流式对话 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/ai")
@Tag(name = "AI 对话", description = "AI 对话与流式响应接口")
public class ChatController {

    private final IChatService chatService;

    public ChatController(IChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发起 AI 流式对话", description = "受保护接口。提交对话内容并通过 SSE 返回流式响应")
    public SseEmitter chat(
            @Valid @RequestBody ChatRequest chatRequest,
            @Parameter(description = "访问令牌，格式为 Bearer <accessToken>")
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "租户ID请求头")
            @RequestHeader(value = MangoContextHeaders.TENANT_ID, required = false) String tenantId,
            @Parameter(description = "兼容租户ID请求头")
            @RequestHeader(value = "TENANT-ID", required = false) String legacyTenantId) {
        Flux<String> events;
        if (hasBearerToken(authorization)) {
            events = chatService.chat(chatRequest, resolveTenantId(tenantId, legacyTenantId));
        } else {
            events = Flux.just(errorEvent("Missing or invalid Authorization header"));
        }
        return AiSseEmitterFactory.createChat(events);
    }

    private String resolveTenantId(String tenantId, String legacyTenantId) {
        if (hasText(tenantId)) {
            return tenantId;
        }
        if (hasText(legacyTenantId)) {
            return legacyTenantId;
        }
        String contextTenantId = MangoContextHolder.tenantId();
        if (hasText(contextTenantId)) {
            return contextTenantId;
        }
        return "default";
    }

    private boolean hasBearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String errorEvent(String message) {
        return "{\"type\":\"error\",\"message\":\"" + message + "\"}";
    }
}
