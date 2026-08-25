package io.mango.ai.api.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.mango.ai.api.enums.AiMessageContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/** 用户发送给 AI 服务的一个内容块。 */
@Getter
@Setter
@Schema(description = "AI 会话输入内容块")
public class AiMessageContentPartCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_TEXT_LENGTH = 20_000;

    @NotNull(message = "内容块类型不能为空")
    @Schema(description = "内容块类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private AiMessageContentType type;

    @Size(max = MAX_TEXT_LENGTH, message = "文本内容长度不能超过20000个字符")
    @Schema(description = "TEXT 内容块的文本")
    private String text;

    @Positive(message = "文件标识必须大于0")
    @Schema(description = "图片、音频、视频或文件内容块的 Mango 文件标识")
    private Long fileId;

    @JsonIgnore
    @AssertTrue(message = "TEXT 内容块必须提供文本，文件内容块必须提供对应文件标识")
    public boolean isContentValid() {
        if (type == null) {
            return true;
        }
        if (type == AiMessageContentType.TEXT) {
            return text != null && !text.isBlank() && fileId == null;
        }
        boolean fileType = type == AiMessageContentType.IMAGE
                || type == AiMessageContentType.AUDIO
                || type == AiMessageContentType.VIDEO
                || type == AiMessageContentType.FILE;
        return fileType && fileId != null && (text == null || text.isBlank());
    }
}
