package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付开放接口收银台入口")
public class PaymentOpenCashierVO {

    @Schema(description = "收银台配置 ID")
    private Long cashierConfigId;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "收银台地址")
    private String cashierUrl;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireTime;
}
