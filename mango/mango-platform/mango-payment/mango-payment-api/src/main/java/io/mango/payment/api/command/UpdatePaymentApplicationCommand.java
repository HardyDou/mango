package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "更新支付接入应用命令")
public class UpdatePaymentApplicationCommand extends SavePaymentApplicationCommand {

    private static final long serialVersionUID = 1L;

    @Override
    @NotNull(message = "应用 ID 不能为空")
    @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long getId() {
        return super.getId();
    }
}
