package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "生成结算汇总命令")
public class GeneratePaymentSettlementSummaryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "结算日期不能为空")
    @Schema(description = "结算日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate settlementDate;

    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码不能超过 64 个字符")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotNull(message = "企业主体 ID 不能为空")
    @Schema(description = "企业主体 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long enterpriseSubjectId;

    @NotBlank(message = "通道编码不能为空")
    @Size(max = 32, message = "通道编码不能超过 32 个字符")
    @Schema(description = "通道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @Schema(description = "是否重新生成已作废汇总")
    private Boolean rebuild;
}
