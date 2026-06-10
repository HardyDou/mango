package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "芒果支付内置虚拟通道支付命令")
public class MangoPayVirtualPaymentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "收银台配置 ID 不能为空")
    @Schema(description = "收银台配置 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cashierConfigId;

    @NotBlank(message = "支付订单号不能为空")
    @Schema(description = "支付订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payOrderNo;

    @NotBlank(message = "付款标题不能为空")
    @Schema(description = "付款标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "付款金额不能为空")
    @Positive(message = "付款金额必须大于 0")
    @Schema(description = "付款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;

    @Schema(description = "标准支付方式编码")
    private String paymentMethodCode;

    @Schema(description = "付款人")
    private String payerName;
}
