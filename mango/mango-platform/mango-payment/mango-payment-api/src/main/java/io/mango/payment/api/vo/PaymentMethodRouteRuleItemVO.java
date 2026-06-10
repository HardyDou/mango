package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "支付方式路由规则明细视图")
public class PaymentMethodRouteRuleItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由明细 ID")
    private Long id;

    @Schema(description = "路由规则 ID")
    private Long ruleId;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "签约配置 ID")
    private Long contractId;

    @Schema(description = "签约名称")
    private String contractName;

    @Schema(description = "支付通道 ID")
    private Long channelId;

    @Schema(description = "支付通道名称")
    private String channelName;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "权重")
    private Integer weight;

    @Schema(description = "最小金额，单位分")
    private Long minAmount;

    @Schema(description = "最大金额，单位分")
    private Long maxAmount;

    @Schema(description = "状态")
    private Integer status;
}
