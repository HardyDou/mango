package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "沙箱支付完成命令")
public class SandboxPaymentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "支付单ID不能为空")
    @Schema(description = "支付单ID")
    private Long paymentOrderId;

    @NotBlank(message = "沙箱事件ID不能为空")
    @Size(max = 128, message = "沙箱事件ID最多128个字符")
    @Schema(description = "沙箱事件ID，用于通道回调幂等")
    private String sandboxEventId;
}
