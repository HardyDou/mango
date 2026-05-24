package io.mango.payment.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class PaymentNotifyCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "支付单ID不能为空")
    private Long paymentOrderId;

    @NotBlank(message = "渠道流水号不能为空")
    private String channelOrderNo;

    @NotBlank(message = "回调事件ID不能为空")
    private String notifyEventId;

    @NotBlank(message = "回调签名不能为空")
    private String signature;
}
