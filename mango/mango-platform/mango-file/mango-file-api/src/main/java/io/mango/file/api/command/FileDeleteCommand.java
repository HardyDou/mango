package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 文件删除命令。
 */
@Data
@Schema(description = "文件删除命令")
public class FileDeleteCommand {

    @NotEmpty(message = "文件ID不能为空")
    @Schema(description = "文件ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull(message = "文件ID不能为空") Long> ids;
}
