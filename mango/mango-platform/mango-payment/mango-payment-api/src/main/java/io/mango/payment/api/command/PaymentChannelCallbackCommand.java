package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付通道标准化回调命令")
public class PaymentChannelCallbackCommand {

    @Schema(description = "回调类型：PAYMENT、REFUND")
    @NotBlank(message = "回调类型不能为空")
    private String callbackType;

    @Schema(description = "通道编码")
    @NotBlank(message = "通道编码不能为空")
    private String channelCode;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "通道退款单号")
    private String channelRefundNo;

    @Schema(description = "通道商户号")
    @NotBlank(message = "通道商户号不能为空")
    private String channelMerchantNo;

    @Schema(description = "通道返回状态：SUCCESS、FAILED、CLOSED、PROCESSING")
    @NotBlank(message = "通道返回状态不能为空")
    private String channelStatus;

    @Schema(description = "通道返回金额，单位分")
    @NotNull(message = "通道返回金额不能为空")
    @Positive(message = "通道返回金额必须大于 0")
    private Long amount;

    @Schema(description = "通道事件时间")
    private LocalDateTime eventTime;

    @Schema(description = "通道返回码")
    private String channelReturnCode;

    @Schema(description = "通道返回信息")
    private String channelMessage;
}
