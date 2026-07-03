package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PDF 合并条目命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PDF 合并条目命令")
public class FileMergePdfEntryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "源文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "源文件ID不能为空")
    private Long fileId;

    @Schema(description = "条目标题，可用于后续书签扩展")
    @Size(max = 255, message = "条目标题长度不能超过255")
    private String title;
}
