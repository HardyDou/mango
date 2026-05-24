package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建业务支付单命令")
public class CreatePayBizOrderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @NotBlank(message = "商户业务单号不能为空")
    @Size(max = 128, message = "商户业务单号最多128个字符")
    private String merchantOrderNo;

    @NotBlank(message = "订单标题不能为空")
    @Size(max = 128, message = "订单标题最多128个字符")
    private String subject;

    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于0")
    private Long amount;

    @Size(max = 16, message = "币种最多16个字符")
    private String currency = "CNY";
}
