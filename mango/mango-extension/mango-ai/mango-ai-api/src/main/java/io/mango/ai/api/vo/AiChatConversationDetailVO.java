package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** AI 对话摘要和完整消息。 */
@Getter
@Setter
public class AiChatConversationDetailVO extends AiChatConversationVO {
    private List<AiChatMessageVO> messages;

    public List<AiChatMessageVO> getMessages() {
        return messages == null ? null : List.copyOf(messages);
    }

    public void setMessages(List<AiChatMessageVO> messages) {
        this.messages = messages == null ? null : List.copyOf(messages);
    }
}
