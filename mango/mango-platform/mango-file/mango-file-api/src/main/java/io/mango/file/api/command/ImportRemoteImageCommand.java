package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** Remote image import command. */
@Data
@Schema(description = "远程图片导入命令")
public class ImportRemoteImageCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "图片地址不能为空")
    @Size(max = 2048, message = "图片地址长度不能超过2048")
    @Schema(description = "待导入的公网 HTTP/HTTPS 图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceUrl;

    @Size(max = 64, message = "业务类型长度不能超过64")
    @Schema(description = "业务类型")
    private String bizType;

    @Size(max = 128, message = "业务ID长度不能超过128")
    @Schema(description = "业务ID")
    private String bizId;

    @Size(max = 4000, message = "业务自定义参数长度不能超过4000")
    @Schema(description = "业务自定义参数 JSON")
    private String bizMeta;

    @PositiveOrZero(message = "逻辑目录ID不能小于0")
    @Schema(description = "逻辑目录ID，根目录为0")
    private Long directoryId;
}
