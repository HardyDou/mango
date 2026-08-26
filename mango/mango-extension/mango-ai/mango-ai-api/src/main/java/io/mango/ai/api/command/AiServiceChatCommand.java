package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** AI 服务会话命令。 */
@Getter
@Setter
@Schema(description = "AI 服务会话命令")
public class AiServiceChatCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_SESSION_ID_LENGTH = 128;

    @NotNull(message = "请求标识不能为空")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "请求标识格式不正确")
    @Schema(description = "客户端为本轮生成的 UUID 请求标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestId;

    @Valid
    @NotNull(message = "消息内容不能为空")
    @Size(min = 1, max = 7, message = "每条消息必须包含1至7个内容块")
    @Schema(description = "文本与正式文件引用组成的消息内容块", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AiMessageContentPartCommand> contentParts;

    @Schema(description = "会话标识；为空时由服务端生成")
    @Size(max = MAX_SESSION_ID_LENGTH, message = "会话标识长度不能超过128个字符")
    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$",
            message = "会话标识只能包含字母、数字、点、下划线和连字符")
    private String sessionId;

    @Schema(description = "本次会话使用的模型标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模型不能为空")
    private Long modelId;

    @Schema(description = "是否启用模型思考模式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "思考模式不能为空")
    private Boolean thinkingEnabled;

    public List<AiMessageContentPartCommand> getContentParts() {
        return contentParts == null ? null : List.copyOf(contentParts);
    }

    public void setContentParts(List<AiMessageContentPartCommand> contentParts) {
        this.contentParts = contentParts == null ? null : List.copyOf(contentParts);
    }
}
