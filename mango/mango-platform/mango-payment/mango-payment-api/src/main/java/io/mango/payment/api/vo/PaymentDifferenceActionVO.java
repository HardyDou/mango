package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对账差异处理动作")
public class PaymentDifferenceActionVO {

    @Schema(description = "处理动作编码")
    private String actionCode;

    @Schema(description = "处理动作名称")
    private String actionName;
}
