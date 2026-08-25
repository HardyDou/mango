package io.mango.ai.core.service;

import io.mango.ai.api.vo.AiMessageContentPartVO;

import java.util.List;

/** 一次需要原子持久化的 AI 用户消息与助手回答。 */
public record AiConversationExchange(
        AiConversationScope scope,
        List<AiMessageContentPartVO> userContentParts,
        List<AiMessageContentPartVO> assistantContentParts,
        boolean thinkingEnabled,
        AiModelResolution resolution) {

    public AiConversationExchange {
        userContentParts = List.copyOf(userContentParts);
        assistantContentParts = List.copyOf(assistantContentParts);
    }
}
