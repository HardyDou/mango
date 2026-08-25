package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** AI 会话请求受理结果。 */
@Getter
@Setter
@Schema(description = "AI 会话请求受理结果")
public class AiServiceChatStartVO {

    @Schema(description = "本次模型调用请求标识")
    private String requestId;

    @Schema(description = "本次对话所属会话标识")
    private String sessionId;
}
