package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件打包条目命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件打包条目命令")
public class FilePackageEntryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "源文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "源文件ID不能为空")
    private Long fileId;

    @Schema(description = "ZIP 内相对路径，例如 合同资料/合同.pdf；支持 ${fileName} 表示源文件记录名",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ZIP内路径不能为空")
    @Size(max = 500, message = "ZIP内路径长度不能超过500")
    private String path;

    @Schema(description = "当前文件压缩档位：NONE、LOW、MEDIUM、HIGH；为空时使用打包命令默认值")
    @Size(max = 16, message = "压缩档位长度不能超过16")
    private String compression;

    @Schema(description = "当前文件目标大小，单位字节；覆盖打包命令 perFileTargetSizeBytes")
    private Long targetSizeBytes;
}
