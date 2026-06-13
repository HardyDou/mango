package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "作废结算汇总命令")
public class VoidPaymentSettlementSummaryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "结算汇总 ID 不能为空")
    @Schema(description = "结算汇总 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "作废原因不能为空")
    @Size(max = 512, message = "作废原因不能超过 512 个字符")
    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String voidReason;
}
