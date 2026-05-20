package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 初始化文件上传命令。
 */
@Data
@Schema(description = "初始化文件上传命令")
public class CreateFileUploadSessionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255")
    private String fileName;

    @Schema(description = "文件大小，单位字节", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    @Schema(description = "文件 SHA-256 哈希")
    @Size(max = 128, message = "文件哈希长度不能超过128")
    private String fileHash;

    @Schema(description = "内容类型")
    @Size(max = 128, message = "内容类型长度不能超过128")
    private String contentType;

    @Schema(description = "分片大小，单位字节")
    @Min(value = 1, message = "分片大小必须大于0")
    private Long chunkSize;

    @Schema(description = "总分片数")
    @Min(value = 1, message = "总分片数必须大于0")
    @Max(value = 10000, message = "总分片数不能超过10000")
    private Integer totalParts;

    @Schema(description = "文件用途，例如 avatar、attachment、contract")
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
}
