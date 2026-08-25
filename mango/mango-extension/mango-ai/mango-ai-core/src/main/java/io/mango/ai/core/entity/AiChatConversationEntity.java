package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** 当前用户在 AI 服务中的持久化对话。 */
@Getter
@Setter
@TableName("ai_chat_conversation")
public class AiChatConversationEntity extends TenantEntity {
    private String sessionId;
    private Long userId;
    private String serviceCode;
    private String title;
    private Long lastModelId;
    private String lastModelName;
    private String lastProviderCode;
    private Boolean lastThinkingEnabled;
    private Integer messageCount;
}
