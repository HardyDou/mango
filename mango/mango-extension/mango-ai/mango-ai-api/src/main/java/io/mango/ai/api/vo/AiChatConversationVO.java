package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 当前用户在指定 AI 服务中的对话摘要。 */
@Getter
@Setter
public class AiChatConversationVO {
    @Schema(description = "会话标识")
    private String sessionId;
    @Schema(description = "会话标题")
    private String title;
    @Schema(description = "最近成功调用的模型标识")
    private Long lastModelId;
    @Schema(description = "最近成功调用的模型名称")
    private String lastModelName;
    @Schema(description = "最近成功调用的供应商编码")
    private String lastProviderCode;
    @Schema(description = "最近一次是否启用思考模式")
    private Boolean lastThinkingEnabled;
    @Schema(description = "会话消息数量")
    private Integer messageCount;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
