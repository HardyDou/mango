package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道签约证书轮换命令")
public class RotatePaymentChannelContractCertificateCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "签约配置 ID 不能为空")
    @Schema(description = "签约配置 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractId;

    @NotNull(message = "签约能力 ID 不能为空")
    @Schema(description = "签约能力 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractCapabilityId;

    @NotBlank(message = "证书字段编码不能为空")
    @Schema(description = "证书文件字段编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String certificateFieldCode;

    @NotNull(message = "新证书文件 ID 不能为空")
    @Schema(description = "新证书文件 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newCertificateFileId;

    @NotNull(message = "证书有效期不能为空")
    @Schema(description = "新证书有效期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime newCertificateExpireTime;

    @NotBlank(message = "轮换原因不能为空")
    @Schema(description = "轮换原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rotateReason;
}
