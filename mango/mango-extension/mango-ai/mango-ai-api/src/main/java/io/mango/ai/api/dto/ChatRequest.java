package io.mango.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Chat request DTO
 *
 * @author Mango
 */
@Data
@Schema(description = "AI 对话请求")
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum message length
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Message content
     */
    @Schema(description = "用户输入的对话内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Message is required")
    @Size(max = MAX_MESSAGE_LENGTH, message = "Message must not exceed " + MAX_MESSAGE_LENGTH + " characters")
    private String message;

    /**
     * Session ID for conversation context (optional)
     */
    @Schema(description = "会话ID，用于保持上下文，可为空")
    private String sessionId;

    /**
     * Enable thinking mode (default true when not specified)
     */
    @Schema(description = "是否启用思考模式，未传时默认启用")
    private Boolean enableThinking;
}
