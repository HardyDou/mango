package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 完成分片上传命令。
 */
@Data
@Schema(description = "完成分片上传命令")
public class CompleteFileUploadPartCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分片序号，从 1 开始", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分片序号不能为空")
    @Min(value = 1, message = "分片序号必须大于0")
    private Integer partNumber;

    @Schema(description = "对象存储返回的 ETag", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ETag不能为空")
    private String etag;

    @Schema(description = "分片大小，单位字节")
    @Min(value = 1, message = "分片大小必须大于0")
    private Long partSize;

    @Schema(description = "分片哈希")
    private String partHash;
}
