package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** AI 对话中的一条已持久化消息。 */
@Getter
@Setter
public class AiChatMessageVO {
    private String role;
    private List<AiMessageContentPartVO> contentParts;
    private Long modelId;
    private String modelName;
    private String providerCode;
    private Boolean thinkingEnabled;
    private LocalDateTime createdAt;
}
