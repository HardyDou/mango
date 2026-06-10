package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "业务订单状态选项")
public class PaymentBusinessOrderStatusVO {

    @Schema(description = "状态编码")
    private String statusCode;

    @Schema(description = "状态名称")
    private String statusName;
}
