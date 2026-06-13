package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道签约能力视图")
public class PaymentChannelContractCapabilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约能力 ID")
    private Long id;

    @Schema(description = "通道能力 ID")
    private Long channelCapabilityId;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "标准支付方式名称")
    private String methodName;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "费率")
    private BigDecimal feeRate;

    @Schema(description = "最小金额，单位分")
    private Long minAmount;

    @Schema(description = "最大金额，单位分")
    private Long maxAmount;

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "证书有效期")
    private LocalDateTime certificateExpireTime;

    @Schema(description = "状态")
    private Integer status;
}
