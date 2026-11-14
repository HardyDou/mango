package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 替换文件内容命令。
 *
 * <p>字段为 null 表示保持原记录值；空字符串表示清空可空字段。文件名扩展名必须与原记录一致。</p>
 */
@Data
@Schema(description = "替换文件内容命令")
public class ReplaceFileContentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件ID不能为空")
    @Positive(message = "文件ID必须大于0")
    private Long id;

    @Schema(description = "替换后的文件名；不传保持原文件名，扩展名必须与原文件一致")
    @Size(max = 255, message = "文件名长度不能超过255")
    private String fileName;

    @Schema(description = "文件用途")
    @Size(max = 64, message = "文件用途长度不能超过64")
    private String purpose;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL")
    @Size(max = 32, message = "访问级别长度不能超过32")
    private String accessLevel;

    @Schema(description = "业务类型")
    @Size(max = 64, message = "业务类型长度不能超过64")
    private String bizType;

    @Schema(description = "业务ID")
    @Size(max = 128, message = "业务ID长度不能超过128")
    private String bizId;

    @Schema(description = "业务自定义参数 JSON")
    @Size(max = 4000, message = "业务自定义参数长度不能超过4000")
    private String bizMeta;

    @Schema(description = "逻辑目录ID。根目录为0")
    @Min(value = 0, message = "逻辑目录ID不能小于0")
    private Long directoryId;
}
