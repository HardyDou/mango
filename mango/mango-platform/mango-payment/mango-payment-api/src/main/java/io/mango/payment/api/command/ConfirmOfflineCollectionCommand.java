package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "确认线下收款到账命令")
public class ConfirmOfflineCollectionCommand {

    @Schema(description = "线下收款 ID")
    @NotNull(message = "线下收款 ID 不能为空")
    private Long id;

    @Schema(description = "确认到账金额，单位分")
    @NotNull(message = "确认到账金额不能为空")
    @Positive(message = "确认到账金额必须大于 0")
    private Long confirmedAmount;

    @Schema(description = "确认说明")
    @Size(max = 512, message = "确认说明长度不能超过 512")
    private String confirmRemark;
}
