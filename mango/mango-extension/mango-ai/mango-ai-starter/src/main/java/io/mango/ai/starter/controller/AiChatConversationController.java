package io.mango.ai.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.AiChatConversationApi;
import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AI 聊天会话 HTTP 适配器。 */
@Validated
@RestController
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the application service; copying a container-managed collaborator is not valid"))
@RequestMapping("/ai/services")
@Tag(name = "AI 聊天会话", description = "当前用户的 AI 聊天会话查询与删除接口")
public class AiChatConversationController implements AiChatConversationApi {

    private final IAiServiceChatService service;

    @Override
    @GetMapping("/conversations")
    @Operation(summary = "查询 AI 聊天会话", description = "查询当前用户在指定 CHAT 服务中的最近会话")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<List<AiChatConversationVO>> conversations(
            @Parameter(description = "AI 服务编码") @RequestParam("serviceCode") String serviceCode) {
        return R.ok(service.conversations(serviceCode));
    }

    @Override
    @GetMapping("/conversation")
    @Operation(summary = "查询 AI 聊天会话消息", description = "查询当前用户指定会话的完整持久化消息")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<AiChatConversationDetailVO> conversation(
            @Parameter(description = "AI 服务编码") @RequestParam("serviceCode") String serviceCode,
            @Parameter(description = "会话标识") @RequestParam("sessionId") String sessionId) {
        return R.ok(service.conversation(serviceCode, sessionId));
    }

    @Override
    @DeleteMapping("/conversation")
    @Operation(summary = "删除 AI 聊天会话", description = "删除当前用户指定会话及其全部消息")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:invoke", desc = "调用 AI 服务")
    public R<Boolean> deleteConversation(
            @Parameter(description = "AI 服务编码") @RequestParam("serviceCode") String serviceCode,
            @Parameter(description = "会话标识") @RequestParam("sessionId") String sessionId) {
        return R.ok(service.deleteConversation(serviceCode, sessionId));
    }
}
