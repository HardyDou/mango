package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "发起支付命令")
public class PayCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "业务支付单ID不能为空")
    private Long bizOrderId;

    @NotBlank(message = "支付方式不能为空")
    @Size(max = 32, message = "支付方式最多32个字符")
    private String payMethod;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键最多128个字符")
    private String idempotencyKey;
}
