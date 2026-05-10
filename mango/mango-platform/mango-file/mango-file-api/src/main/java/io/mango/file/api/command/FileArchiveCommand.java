package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件归档命令。
 */
@Data
@Schema(description = "文件归档命令")
public class FileArchiveCommand {

    @NotNull(message = "文件ID不能为空")
    @Schema(description = "文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "归档原因")
    private String reason;
}
