package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "确认线下银行流水匹配到账命令")
public class ConfirmOfflineBankStatementMatchCommand {

    @Schema(description = "银行流水明细 ID")
    @NotEmpty(message = "银行流水明细 ID 不能为空")
    private List<Long> itemIds;

    @Schema(description = "确认说明")
    @Size(max = 512, message = "确认说明长度不能超过 512")
    private String confirmRemark;
}
