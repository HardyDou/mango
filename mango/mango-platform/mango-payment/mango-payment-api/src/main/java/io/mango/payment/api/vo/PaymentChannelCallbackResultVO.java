package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付通道标准化回调处理结果")
public class PaymentChannelCallbackResultVO {

    @Schema(description = "本次是否推进本地状态")
    private Boolean changed;

    @Schema(description = "本地订单号")
    private String orderNo;

    @Schema(description = "处理后的本地状态")
    private String status;

    @Schema(description = "关联交易流水号")
    private String flowNo;

    @Schema(description = "处理结果说明")
    private String message;
}
