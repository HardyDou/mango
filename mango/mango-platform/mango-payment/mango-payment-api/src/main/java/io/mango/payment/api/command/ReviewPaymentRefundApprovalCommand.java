package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "审核后台退款审批命令")
public class ReviewPaymentRefundApprovalCommand {

    @Schema(description = "退款审批 ID")
    @NotNull(message = "退款审批 ID 不能为空")
    private Long id;

    @Schema(description = "审核动作，APPROVE-通过，REJECT-拒绝")
    @NotBlank(message = "审核动作不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "审核动作只能为 APPROVE 或 REJECT")
    private String action;

    @Schema(description = "审核说明")
    @NotBlank(message = "审核说明不能为空")
    @Size(max = 512, message = "审核说明长度不能超过 512")
    private String reviewReason;
}
