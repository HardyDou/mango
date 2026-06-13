package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存通道账单获取源配置命令")
public class SavePaymentChannelBillSourceCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置 ID，更新时必填")
    private Long id;

    @NotNull(message = "签约通道不能为空")
    @Schema(description = "签约通道 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractId;

    @NotBlank(message = "获取方式不能为空")
    @Size(max = 16, message = "获取方式不能超过 16 个字符")
    @Schema(description = "获取方式：MANUAL、FTP、FTPS、HTTP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fetchMode;

    @Size(max = 255, message = "接口地址不能超过 255 个字符")
    @Schema(description = "HTTP 获取地址或 FTP/FTPS 服务器地址")
    private String endpoint;

    @Size(max = 255, message = "远端路径不能超过 255 个字符")
    @Schema(description = "FTP/FTPS 远端路径")
    private String remotePath;

    @Size(max = 255, message = "认证配置引用不能超过 255 个字符")
    @Schema(description = "认证配置引用，不保存明文密钥")
    private String credentialRef;

    @Size(max = 32, message = "分页模式不能超过 32 个字符")
    @Schema(description = "HTTP 分页模式：PAGE、CURSOR")
    private String pageMode;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer enabled;
}
