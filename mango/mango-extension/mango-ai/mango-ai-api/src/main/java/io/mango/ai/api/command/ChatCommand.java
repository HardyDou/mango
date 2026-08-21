package io.mango.ai.api.command;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * AI 对话命令。
 */
@Getter
@Setter
@Schema(description = "AI 对话命令")
public class ChatCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_SESSION_ID_LENGTH = 128;

    @Schema(description = "用户输入的对话内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "对话内容不能为空")
    @Size(max = MAX_MESSAGE_LENGTH, message = "对话内容长度不能超过2000个字符")
    private String message;

    @Schema(description = "会话标识；为空时由服务端生成")
    @Size(max = MAX_SESSION_ID_LENGTH, message = "会话标识长度不能超过128个字符")
    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$",
            message = "会话标识只能包含字母、数字、点、下划线和连字符")
    private String sessionId;

    @Schema(description = "是否返回模型思考内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "思考模式不能为空")
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean enableThinking = Boolean.TRUE;
}
