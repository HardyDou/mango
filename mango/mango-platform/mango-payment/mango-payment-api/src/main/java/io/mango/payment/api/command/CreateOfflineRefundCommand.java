package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建线下退款命令")
public class CreateOfflineRefundCommand {

    @Schema(description = "线下收款 ID")
    @NotNull(message = "线下收款 ID 不能为空")
    private Long offlineCollectionId;

    @Schema(description = "退款金额，单位分")
    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须大于 0")
    private Long refundAmount;

    @Schema(description = "退款账户户名")
    @NotBlank(message = "退款账户户名不能为空")
    @Size(max = 128, message = "退款账户户名长度不能超过 128")
    private String refundAccountName;

    @Schema(description = "退款账号")
    @NotBlank(message = "退款账号不能为空")
    @Size(max = 128, message = "退款账号长度不能超过 128")
    private String refundAccountNo;

    @Schema(description = "退款开户行")
    @NotBlank(message = "退款开户行不能为空")
    @Size(max = 128, message = "退款开户行长度不能超过 128")
    private String refundBankName;

    @Schema(description = "退款凭证文件 ID，多个用英文逗号分隔")
    @NotBlank(message = "退款凭证不能为空")
    @Size(max = 512, message = "退款凭证长度不能超过 512")
    private String refundVoucherFileIds;

    @Schema(description = "退款原因")
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 512, message = "退款原因长度不能超过 512")
    private String reason;

    @Schema(description = "退款备注")
    @Size(max = 512, message = "退款备注长度不能超过 512")
    private String remark;
}
