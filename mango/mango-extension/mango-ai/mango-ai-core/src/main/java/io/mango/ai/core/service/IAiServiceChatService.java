package io.mango.ai.core.service;

import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import reactor.core.publisher.Flux;

import java.util.List;

/** 所有可运行 AI 服务的统一流式会话能力。 */
public interface IAiServiceChatService {

    List<AiChatConversationVO> conversations(String serviceCode);

    AiChatConversationDetailVO conversation(String serviceCode, String sessionId);

    Boolean deleteConversation(String serviceCode, String sessionId);

    AiServiceRuntimeOptionsVO options(String serviceCode);

    /**
     * 以会话形式调用指定 AI 服务。
     *
     * @param serviceCode 服务编码
     * @param command 聊天命令
     * @return 标准 JSON 事件流
     */
    Flux<String> chat(String serviceCode, AiServiceChatCommand command);
}
