package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 保存系统生成文件命令。
 */
@Data
@Schema(description = "保存系统生成文件命令")
public class SaveGeneratedFileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "文件输入流不能为空")
    @Schema(description = "文件输入流", requiredMode = Schema.RequiredMode.REQUIRED)
    private transient InputStream inputStream;

    @NotBlank(message = "原始文件名不能为空")
    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @NotBlank(message = "内容类型不能为空")
    @Schema(description = "内容类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentType;

    @Schema(description = "文件用途，例如 template-render")
    private String purpose;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;
}
