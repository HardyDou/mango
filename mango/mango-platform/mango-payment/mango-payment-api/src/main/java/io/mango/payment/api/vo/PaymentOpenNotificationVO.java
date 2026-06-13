package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付开放接口业务通知")
public class PaymentOpenNotificationVO {

    @Schema(description = "通知单号")
    private String notifyNo;

    @Schema(description = "通知类型：PAYMENT_SUCCESS、PAYMENT_FAILED、PAYMENT_CLOSED、REFUND_SUCCESS、REFUND_FAILED")
    private String notificationType;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "业务退款单号")
    private String bizRefundNo;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "支付金额，单位分")
    private Long amount;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "支付或退款状态")
    private String status;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "通道退款单号")
    private String channelRefundNo;

    @Schema(description = "交易流水号")
    private String flowNo;

    @Schema(description = "事件发生时间")
    private String eventTime;

    @Schema(description = "通知发送时间")
    private String notifyTime;

    @Schema(description = "签名算法")
    private String signAlgorithm;

    @Schema(description = "通知签名")
    private String signature;
}
