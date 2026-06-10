package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "支付开放接口发起退款命令")
public class CreatePaymentOpenRefundCommand {

    @Schema(description = "租户 ID")
    @NotNull(message = "租户 ID 不能为空")
    private Long tenantId;

    @Schema(description = "支付应用 AppId")
    @NotBlank(message = "AppId 不能为空")
    @Size(max = 64, message = "AppId 长度不能超过 64")
    private String appId;

    @Schema(description = "业务订单号")
    @NotBlank(message = "业务订单号不能为空")
    @Size(max = 64, message = "业务订单号长度不能超过 64")
    private String bizOrderNo;

    @Schema(description = "业务退款单号")
    @NotBlank(message = "业务退款单号不能为空")
    @Size(max = 64, message = "业务退款单号长度不能超过 64")
    private String bizRefundNo;

    @Schema(description = "退款金额，单位分")
    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须大于 0")
    private Long refundAmount;

    @Schema(description = "退款原因")
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 512, message = "退款原因长度不能超过 512")
    private String reason;
}
