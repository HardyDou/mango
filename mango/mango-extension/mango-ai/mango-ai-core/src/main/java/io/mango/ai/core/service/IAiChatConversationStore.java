package io.mango.ai.core.service;

import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import java.util.List;

/** AI 聊天会话与消息的持久化端口。 */
public interface IAiChatConversationStore {

    List<AiChatConversationVO> list(AiConversationScope scope);

    AiChatConversationDetailVO detail(AiConversationScope scope);

    ConversationState load(AiConversationScope scope, int maxHistoryMessages);

    void saveExchange(AiConversationExchange exchange);

    boolean delete(AiConversationScope scope);

    record ConversationMessage(String role, List<AiMessageContentPartVO> contentParts) {
        public ConversationMessage {
            contentParts = List.copyOf(contentParts);
        }
    }

    record ConversationState(List<ConversationMessage> messages) {
        public ConversationState {
            messages = List.copyOf(messages);
        }
    }
}
