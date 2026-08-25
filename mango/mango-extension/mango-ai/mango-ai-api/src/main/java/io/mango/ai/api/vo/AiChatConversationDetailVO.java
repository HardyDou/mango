package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** AI 对话摘要和完整消息。 */
@Getter
@Setter
public class AiChatConversationDetailVO extends AiChatConversationVO {
    private List<AiChatMessageVO> messages;
}
