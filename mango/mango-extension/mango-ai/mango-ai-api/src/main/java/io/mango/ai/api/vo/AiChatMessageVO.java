package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** AI 对话中的一条已持久化消息。 */
@Getter
@Setter
public class AiChatMessageVO {
    @Schema(description = "消息角色")
    private String role;
    @Schema(description = "消息内容块列表")
    private List<AiMessageContentPartVO> contentParts;
    @Schema(description = "模型标识")
    private Long modelId;
    @Schema(description = "供应商侧模型标识")
    private String modelName;
    @Schema(description = "供应商编码")
    private String providerCode;
    @Schema(description = "本轮是否启用思考模式")
    private Boolean thinkingEnabled;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public List<AiMessageContentPartVO> getContentParts() {
        return contentParts == null ? null : List.copyOf(contentParts);
    }

    public void setContentParts(List<AiMessageContentPartVO> contentParts) {
        this.contentParts = contentParts == null ? null : List.copyOf(contentParts);
    }
}
