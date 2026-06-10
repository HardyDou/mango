package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付通知状态选项")
public class PaymentNotificationStatusVO {

    @Schema(description = "状态编码")
    private String statusCode;

    @Schema(description = "状态名称")
    private String statusName;
}
