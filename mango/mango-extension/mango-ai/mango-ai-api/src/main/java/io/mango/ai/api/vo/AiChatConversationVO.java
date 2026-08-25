package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 当前用户在指定 AI 服务中的对话摘要。 */
@Getter
@Setter
public class AiChatConversationVO {
    private String sessionId;
    private String title;
    private Long lastModelId;
    private String lastModelName;
    private String lastProviderCode;
    private Boolean lastThinkingEnabled;
    private Integer messageCount;
    private LocalDateTime updatedAt;
}
