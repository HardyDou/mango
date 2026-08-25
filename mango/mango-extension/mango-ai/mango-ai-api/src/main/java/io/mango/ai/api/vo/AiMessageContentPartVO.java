package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiMessageContentType;
import lombok.Getter;
import lombok.Setter;

/** AI 会话中已校验并持久化的内容块。 */
@Getter
@Setter
public class AiMessageContentPartVO {
    private AiMessageContentType type;
    private String text;
    private String dataJson;
    private Long fileId;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
