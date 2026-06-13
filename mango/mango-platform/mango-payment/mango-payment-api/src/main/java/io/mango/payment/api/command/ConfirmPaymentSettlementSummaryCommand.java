package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "确认结算汇总命令")
public class ConfirmPaymentSettlementSummaryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "结算汇总 ID 不能为空")
    @Schema(description = "结算汇总 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
