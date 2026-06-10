package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收银台订单视图")
public class PaymentCashierOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "商品或订单名称")
    private String orderTitle;

    @Schema(description = "订单金额，单位分")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
