package io.mango.ai.starter.controller;

import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.core.service.IAiServiceChatService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import io.mango.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 所有可运行 AI 服务的统一流式 HTTP 入口。 */
@Validated
@RestController
@RequestMapping("/ai/services")
@RequiredArgsConstructor
@Tag(name = "AI 服务运行", description = "所有可运行 AI 服务的统一流式会话接口")
public class AiServiceChatController {

    private final IAiServiceChatService service;

    @GetMapping("/options")
    @Operation(summary = "查询 AI 服务运行选项", description = "查询指定服务可使用的模型和模态能力")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<AiServiceRuntimeOptionsVO> options(
            @Parameter(description = "AI 服务编码")
            @RequestParam("serviceCode")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode) {
        return R.ok(service.options(serviceCode));
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "以会话形式调用 AI 服务", description = "加载服务绑定的 Prompt、Skill、Schema 和会话指定模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public SseEmitter chat(
            @Parameter(description = "AI 服务编码")
            @RequestParam("serviceCode")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode,
            @Valid @RequestBody AiServiceChatCommand command) {
        return AiSseEmitterFactory.create(service.chat(serviceCode, command));
    }
}
