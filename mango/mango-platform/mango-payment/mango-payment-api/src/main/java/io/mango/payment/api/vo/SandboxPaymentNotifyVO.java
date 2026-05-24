package io.mango.payment.api.vo;

import io.mango.payment.api.command.PaymentNotifyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "沙箱支付回调报文")
public class SandboxPaymentNotifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "沙箱通道编码")
    private String channelCode;

    @Schema(description = "支付单ID")
    private Long paymentOrderId;

    @Schema(description = "沙箱渠道流水号")
    private String channelOrderNo;

    @Schema(description = "沙箱回调事件ID")
    private String notifyEventId;

    @Schema(description = "沙箱回调签名")
    private String signature;

    @Schema(description = "可直接提交给支付回调接口的命令")
    private PaymentNotifyCommand notifyCommand;
}
