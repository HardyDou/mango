package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "生成本地订单通道核验命令")
public class GeneratePaymentLocalOrderCheckCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "通道编码不能为空")
    @Size(max = 32, message = "通道编码不能超过 32 个字符")
    @Schema(description = "通道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @NotNull(message = "签约通道 ID 不能为空")
    @Schema(description = "签约通道 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractId;

    @NotNull(message = "账单日期不能为空")
    @Schema(description = "账单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billDate;
}
