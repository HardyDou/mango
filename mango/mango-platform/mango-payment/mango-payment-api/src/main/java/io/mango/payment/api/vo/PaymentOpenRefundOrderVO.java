package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付开放接口退款订单")
public class PaymentOpenRefundOrderVO {

    @Schema(description = "退款订单 ID")
    private Long id;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "业务退款单号")
    private String bizRefundNo;

    @Schema(description = "原支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "原支付订单号")
    private String payOrderNo;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款状态")
    private String status;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "通道退款单号")
    private String channelRefundNo;

    @Schema(description = "退款成功时间")
    private LocalDateTime refundTime;

    @Schema(description = "交易流水号")
    private String flowNo;
}
