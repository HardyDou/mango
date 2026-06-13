package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "提交线下转账凭证命令")
public class SubmitOfflineTransferVoucherCommand {

    @Schema(description = "支付订单号")
    @NotBlank(message = "支付订单号不能为空")
    @Size(max = 64, message = "支付订单号长度不能超过 64")
    private String payOrderNo;

    @Schema(description = "实际转账金额，单位分")
    @NotNull(message = "实际转账金额不能为空")
    @Positive(message = "实际转账金额必须大于 0")
    private Long transferAmount;

    @Schema(description = "转账凭证文件 ID，多个用英文逗号分隔")
    @NotBlank(message = "转账凭证不能为空")
    @Size(max = 512, message = "转账凭证长度不能超过 512")
    private String voucherFileIds;

    @Schema(description = "用户提交说明")
    @Size(max = 512, message = "用户提交说明长度不能超过 512")
    private String submitRemark;
}
