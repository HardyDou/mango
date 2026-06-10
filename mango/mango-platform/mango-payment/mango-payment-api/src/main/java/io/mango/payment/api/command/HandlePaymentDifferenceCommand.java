package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "处理对账差异命令")
public class HandlePaymentDifferenceCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "对账差异 ID 不能为空")
    @Schema(description = "对账差异 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "处理动作不能为空")
    @Size(max = 64, message = "处理动作不能超过 64 个字符")
    @Schema(description = "处理动作", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processAction;

    @NotBlank(message = "处理原因不能为空")
    @Size(max = 512, message = "处理原因不能超过 512 个字符")
    @Schema(description = "处理原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processReason;

    @NotBlank(message = "处理结果不能为空")
    @Size(max = 512, message = "处理结果不能超过 512 个字符")
    @Schema(description = "处理结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processResult;

    @Size(max = 512, message = "处理凭据不能超过 512 个字符")
    @Schema(description = "处理凭据文件 ID 或业务凭据 token")
    private String processEvidence;
}
