package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 对话中按顺序持久化的消息。 */
@Getter
@Setter
@TableName("ai_chat_message")
public class AiChatMessageEntity extends TenantEntity {
    private Long conversationId;
    private Integer sequenceNo;
    private String role;
    private String contentPartsJson;
    private Long modelId;
    private String modelName;
    private String providerCode;
    private Boolean thinkingEnabled;
}
