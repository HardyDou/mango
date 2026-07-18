package io.mango.file.api.command;

import io.mango.common.contract.BinaryTransferContract;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 内部服务保存文件命令。
 */
@Data
@BinaryTransferContract
@Schema(description = "内部服务保存文件命令")
public class SaveFileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件输入流")
    @NotNull(message = "文件输入流不能为空")
    private transient InputStream inputStream;

    @Schema(description = "原始文件名")
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255")
    private String fileName;

    @Schema(description = "文件大小，单位字节")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    @Schema(description = "内容类型")
    @Size(max = 128, message = "内容类型长度不能超过128")
    private String contentType;

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
    @PositiveOrZero(message = "逻辑目录ID不能小于0")
    private Long directoryId;
}
