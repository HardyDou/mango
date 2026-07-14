package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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
    @Size(max = 64, message = "支付订单号不能超过 64 个字符")
    private String payOrderNo;

    @Schema(description = "通道交易号")
    @Size(max = 128, message = "通道交易号不能超过 128 个字符")
    private String channelTradeNo;

    @Schema(description = "退款订单号")
    @Size(max = 64, message = "退款订单号不能超过 64 个字符")
    private String refundOrderNo;

    @Schema(description = "通道退款单号")
    @Size(max = 128, message = "通道退款单号不能超过 128 个字符")
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
    @PastOrPresent(message = "通道事件时间不能晚于当前时间")
    private LocalDateTime eventTime;

    @Schema(description = "通道返回码")
    @Size(max = 64, message = "通道返回码不能超过 64 个字符")
    private String channelReturnCode;

    @Schema(description = "通道返回信息")
    @Size(max = 1024, message = "通道返回信息不能超过 1024 个字符")
    private String channelMessage;
}
