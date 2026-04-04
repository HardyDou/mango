package io.mango.ai.api.dto;

import io.mango.common.vo.BaseVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Chat request DTO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatRequest extends BaseVO {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum message length
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Message content
     */
    @NotBlank(message = "Message is required")
    @Size(max = MAX_MESSAGE_LENGTH, message = "Message must not exceed " + MAX_MESSAGE_LENGTH + " characters")
    private String message;

    /**
     * Session ID for conversation context (optional)
     */
    private String sessionId;

    /**
     * Enable thinking mode (default true when not specified)
     */
    private Boolean enableThinking;
}
