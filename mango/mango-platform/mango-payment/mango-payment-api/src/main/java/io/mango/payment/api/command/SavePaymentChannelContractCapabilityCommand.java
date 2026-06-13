package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "保存通道签约能力命令")
public class SavePaymentChannelContractCapabilityCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约能力 ID")
    private Long id;

    @NotNull(message = "通道能力不能为空")
    @Schema(description = "通道能力 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long channelCapabilityId;

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

    @NotNull(message = "签约能力状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
