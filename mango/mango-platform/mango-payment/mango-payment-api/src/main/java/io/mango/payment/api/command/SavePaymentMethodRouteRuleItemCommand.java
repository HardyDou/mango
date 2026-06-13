package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存支付方式路由规则明细命令")
public class SavePaymentMethodRouteRuleItemCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由明细 ID")
    private Long id;

    @NotNull(message = "签约能力不能为空")
    @Schema(description = "签约能力 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractCapabilityId;

    @Schema(description = "优先级，数值越小越优先")
    private Integer priority;

    @Schema(description = "权重")
    private Integer weight;

    @Schema(description = "最小金额，单位分")
    private Long minAmount;

    @Schema(description = "最大金额，单位分")
    private Long maxAmount;

    @NotNull(message = "路由明细状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
