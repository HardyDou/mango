package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "退款订单")
public class PaymentRefundOrderVO {

    @Schema(description = "退款订单 ID")
    private Long id;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "业务退款单号")
    private String bizRefundNo;

    @Schema(description = "原支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "签约 ID")
    private Long contractId;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "原支付订单号")
    private String payOrderNo;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付标题")
    private String title;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付方式名称")
    private String methodName;

    @Schema(description = "实际支付通道编码")
    private String channelCode;

    @Schema(description = "实际支付通道名称")
    private String channelName;

    @Schema(description = "通道商户号")
    private String channelMerchantNo;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "通道退款单号")
    private String channelRefundNo;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款订单状态编码")
    private String status;

    @Schema(description = "退款订单状态名称")
    private String statusName;

    @Schema(description = "退款成功时间")
    private LocalDateTime refundTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "关联交易流水号")
    private String flowNo;

    @Schema(description = "状态流转记录")
    private List<PaymentOrderStatusFlowVO> statusFlows;
}
