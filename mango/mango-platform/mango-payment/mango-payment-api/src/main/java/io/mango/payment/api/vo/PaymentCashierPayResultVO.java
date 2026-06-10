package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收银台支付结果视图")
public class PaymentCashierPayResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "交易流水号")
    private String flowNo;

    @Schema(description = "支付状态")
    private String status;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "支付通道名称")
    private String channelName;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付方式名称")
    private String methodName;

    @Schema(description = "金额，单位分")
    private Long amount;

    @Schema(description = "完成时间")
    private LocalDateTime paidTime;

    @Schema(description = "支付物料")
    private PaymentCashierPayMaterialVO material;
}
