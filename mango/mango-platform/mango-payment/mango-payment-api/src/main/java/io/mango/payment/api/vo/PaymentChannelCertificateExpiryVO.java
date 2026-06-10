package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道签约证书到期提醒视图")
public class PaymentChannelCertificateExpiryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约配置 ID")
    private Long contractId;

    @Schema(description = "签约名称")
    private String contractName;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "证书有效期")
    private LocalDateTime certificateExpireTime;

    @Schema(description = "距离到期天数")
    private Long daysToExpire;
}
