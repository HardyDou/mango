package io.mango.ai.core.controller;

import io.mango.ai.core.service.IAiPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 模块 SSE 推送 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/ai")
@Tag(name = "AI 推送", description = "AI 模块 SSE 连接与消息推送接口")
public class SseController {

    private final IAiPushService aiPushService;

    public SseController(IAiPushService aiPushService) {
        this.aiPushService = aiPushService;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 AI SSE 连接", description = "受保护接口。建立 AI 模块服务端事件推送连接")
    public SseEmitter connect() {
        return AiSseEmitterFactory.createPush(aiPushService.connect());
    }
}
