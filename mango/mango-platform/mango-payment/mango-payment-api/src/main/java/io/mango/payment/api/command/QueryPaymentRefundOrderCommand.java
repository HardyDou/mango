package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "退款订单主动查询命令")
public class QueryPaymentRefundOrderCommand {

    @NotNull(message = "退款订单 ID 不能为空")
    @Schema(description = "退款订单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
