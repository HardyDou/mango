package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道账单明细导入命令")
public class PaymentReconciliationBillItemCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "通道交易号不能为空")
    @Size(max = 128, message = "通道交易号不能超过 128 个字符")
    @Schema(description = "通道交易号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelTradeNo;

    @NotBlank(message = "交易类型不能为空")
    @Size(max = 32, message = "交易类型不能超过 32 个字符")
    @Schema(description = "交易类型：PAYMENT、REFUND、FEE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tradeType;

    @NotNull(message = "金额不能为空")
    @PositiveOrZero(message = "金额不能为负数")
    @Schema(description = "金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;

    @NotNull(message = "手续费不能为空")
    @PositiveOrZero(message = "手续费不能为负数")
    @Schema(description = "手续费，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fee;

    @NotNull(message = "通道交易时间不能为空")
    @Schema(description = "通道交易时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime tradeTime;
}
