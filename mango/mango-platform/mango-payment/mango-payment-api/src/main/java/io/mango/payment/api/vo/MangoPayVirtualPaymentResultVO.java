package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "芒果支付内置虚拟通道支付结果")
public class MangoPayVirtualPaymentResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "芒果支付内置虚拟通道支付单号")
    private String virtualPaymentNo;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "支付状态")
    private String status;

    @Schema(description = "付款标题")
    private String title;

    @Schema(description = "付款金额，单位分")
    private Long amount;

    @Schema(description = "完成时间")
    private LocalDateTime paidTime;
}
