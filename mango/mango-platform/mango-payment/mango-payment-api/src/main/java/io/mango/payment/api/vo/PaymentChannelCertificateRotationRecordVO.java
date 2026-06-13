package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道签约证书轮换记录视图")
public class PaymentChannelCertificateRotationRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "签约配置 ID")
    private Long contractId;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "证书文件字段编码")
    private String certificateFieldCode;

    @Schema(description = "旧证书文件 ID")
    private Long oldCertificateFileId;

    @Schema(description = "新证书文件 ID")
    private Long newCertificateFileId;

    @Schema(description = "旧证书有效期")
    private LocalDateTime oldCertificateExpireTime;

    @Schema(description = "新证书有效期")
    private LocalDateTime newCertificateExpireTime;

    @Schema(description = "轮换原因")
    private String rotateReason;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "轮换时间")
    private LocalDateTime rotateTime;
}
