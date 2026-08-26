package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiMessageContentType;
import lombok.Getter;
import lombok.Setter;

/** AI 会话中已校验并持久化的内容块。 */
@Getter
@Setter
public class AiMessageContentPartVO {
    @Schema(description = "内容块类型")
    private AiMessageContentType type;
    @Schema(description = "文本内容")
    private String text;
    @Schema(description = "结构化数据 JSON")
    private String dataJson;
    @Schema(description = "Mango 文件标识")
    private Long fileId;
    @Schema(description = "文件名称")
    private String fileName;
    @Schema(description = "文件 MIME 类型")
    private String contentType;
    @Schema(description = "文件字节数")
    private Long fileSize;
}
