package io.mango.ai.core.service;

import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.api.vo.AiServiceChatStartVO;

import java.util.List;

/** 所有可运行 AI 服务的统一流式会话能力。 */
public interface IAiServiceChatService {

    List<AiChatConversationVO> conversations(String serviceCode);

    AiChatConversationDetailVO conversation(String serviceCode, String sessionId);

    Boolean deleteConversation(String serviceCode, String sessionId);

    AiServiceRuntimeOptionsVO options(String serviceCode);

    /** 受理一次 AI 会话调用，增量结果通过 Mango Realtime 推送。 */
    AiServiceChatStartVO chat(String serviceCode, AiServiceChatCommand command);

    /** 取消当前用户发起且仍在执行的 AI 会话调用。 */
    Boolean cancel(String requestId);
}
