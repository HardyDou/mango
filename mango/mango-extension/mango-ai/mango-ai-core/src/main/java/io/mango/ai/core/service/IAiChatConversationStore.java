package io.mango.ai.core.service;

import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.core.entity.AiChatConversationEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** AI 聊天会话与消息的持久化端口。 */
public interface IAiChatConversationStore {

    List<AiChatConversationVO> list(String tenantId, Long userId, String serviceCode);

    AiChatConversationDetailVO detail(String tenantId, Long userId, String serviceCode, String sessionId);

    ConversationState load(
            String tenantId,
            Long userId,
            String serviceCode,
            String sessionId,
            int maxHistoryMessages);

    void saveExchange(
            String tenantId,
            Long userId,
            String serviceCode,
            String sessionId,
            List<AiMessageContentPartVO> userContentParts,
            List<AiMessageContentPartVO> assistantContentParts,
            boolean thinkingEnabled,
            AiModelResolution resolution);

    boolean delete(String tenantId, Long userId, String serviceCode, String sessionId);

    record ConversationMessage(String role, List<AiMessageContentPartVO> contentParts) {
        public ConversationMessage {
            contentParts = List.copyOf(contentParts);
        }
    }

    record ConversationState(
            AiChatConversationEntity conversation,
            List<ConversationMessage> messages) {
        public ConversationState {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
    }
}
