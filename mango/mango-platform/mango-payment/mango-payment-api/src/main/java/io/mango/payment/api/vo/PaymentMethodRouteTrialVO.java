package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "支付方式路由试算结果")
public class PaymentMethodRouteTrialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否命中")
    private Boolean matched;

    @Schema(description = "命中的路由规则")
    private PaymentMethodRouteRuleVO matchedRule;

    @Schema(description = "命中的路由明细")
    private PaymentMethodRouteRuleItemVO matchedItem;

    @Schema(description = "过滤原因")
    private List<String> filterReasons = new ArrayList<>();
}
