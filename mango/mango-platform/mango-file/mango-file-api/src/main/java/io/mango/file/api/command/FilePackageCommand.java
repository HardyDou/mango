package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文件打包命令。
 */
@Data
@Schema(description = "文件打包命令")
public class FilePackageCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "输出 ZIP 文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "输出文件名不能为空")
    @Size(max = 255, message = "输出文件名长度不能超过255")
    private String fileName;

    @Schema(description = "文件用途，例如 attachment、contract-package")
    @Size(max = 64, message = "文件用途长度不能超过64")
    private String purpose;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL。默认 PRIVATE")
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
    private Long directoryId;

    @Schema(description = "打包内文件默认压缩档位：NONE、LOW、MEDIUM、HIGH。默认 NONE")
    @Size(max = 16, message = "压缩档位长度不能超过16")
    private String compression;

    @Schema(description = "打包内每个可压缩文件的默认目标大小，单位字节；不表示 ZIP 总大小")
    private Long perFileTargetSizeBytes;

    @Valid
    @NotEmpty(message = "打包文件清单不能为空")
    @Schema(description = "打包文件清单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FilePackageEntryCommand> entries;
}
