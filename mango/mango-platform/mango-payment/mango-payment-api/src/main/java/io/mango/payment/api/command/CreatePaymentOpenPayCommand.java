package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "支付开放接口发起支付命令")
public class CreatePaymentOpenPayCommand {

    @Schema(description = "标准支付方式编码")
    @NotBlank(message = "支付方式编码不能为空")
    @Size(max = 64, message = "支付方式编码长度不能超过 64")
    private String methodCode;
}
