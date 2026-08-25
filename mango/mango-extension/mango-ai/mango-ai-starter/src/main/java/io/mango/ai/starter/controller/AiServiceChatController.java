package io.mango.ai.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.AiServiceChatApi;
import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.vo.AiServiceChatStartVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.core.service.IAiServiceChatService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 所有可运行 AI 服务的统一会话受理入口。 */
@Validated
@RestController
@RequestMapping("/ai/services")
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the application service; copying a container-managed collaborator is not valid"))
@Tag(name = "AI 服务运行", description = "所有可运行 AI 服务的统一流式会话接口")
public class AiServiceChatController implements AiServiceChatApi {

    private final IAiServiceChatService service;

    @Override
    @GetMapping("/options")
    @Operation(summary = "查询 AI 服务运行选项", description = "查询指定服务可使用的模型和模态能力")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<AiServiceRuntimeOptionsVO> options(
            @Parameter(description = "AI 服务编码")
            @RequestParam("serviceCode")
            String serviceCode) {
        return R.ok(service.options(serviceCode));
    }

    @Override
    @PostMapping("/chat")
    @Operation(summary = "以会话形式调用 AI 服务", description = "加载服务绑定的 Prompt、Skill、Schema 和会话指定模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<AiServiceChatStartVO> chat(
            @Parameter(description = "AI 服务编码")
            @RequestParam("serviceCode")
            String serviceCode,
            @RequestBody AiServiceChatCommand command) {
        return R.ok(service.chat(serviceCode, command));
    }

    @Override
    @DeleteMapping("/chat")
    @Operation(summary = "取消 AI 服务调用", description = "取消当前用户发起且仍在执行的模型调用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<Boolean> cancel(
            @Parameter(description = "本次模型调用请求标识")
            @RequestParam("requestId")
            String requestId) {
        return R.ok(service.cancel(requestId));
    }
}
