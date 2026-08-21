package io.mango.ai.starter.controller;

import io.mango.ai.api.command.ChatCommand;
import io.mango.ai.core.service.IChatService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 流式对话 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "AI 对话与流式响应接口")
public class ChatController {

    private final IChatService chatService;

    /**
     * 发起 AI 流式对话。
     *
     * @param command 对话命令
     * @return SSE 响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "发起 AI 流式对话",
            description = "校验登录用户的 AI 对话权限，并在当前租户内通过 SSE 返回模型响应")
    @ApiAccess(
            mode = ApiResourceAccessMode.PERMISSION,
            permission = "ai:chat:use",
            desc = "使用 AI 对话")
    public SseEmitter chat(@Valid @RequestBody ChatCommand command) {
        return AiSseEmitterFactory.createChat(chatService.chat(command));
    }
}
