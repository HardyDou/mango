package io.mango.payment.api.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefundCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "业务支付单ID不能为空")
    private Long bizOrderId;

    @NotBlank(message = "商户退款单号不能为空")
    @Size(max = 128, message = "商户退款单号最多128个字符")
    private String merchantRefundNo;

    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于0")
    private Long refundAmount;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键最多128个字符")
    private String idempotencyKey;
}
