package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "导入通道账单对账命令")
public class ImportPaymentReconciliationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "通道编码不能为空")
    @Size(max = 32, message = "通道编码不能超过 32 个字符")
    @Schema(description = "通道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @NotNull(message = "账单日期不能为空")
    @Schema(description = "账单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billDate;

    @Schema(description = "账单文件 ID")
    private Long billFileId;

    @NotBlank(message = "账单文件名不能为空")
    @Size(max = 255, message = "账单文件名不能超过 255 个字符")
    @Schema(description = "账单文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String billFileName;

    @NotBlank(message = "账单文件摘要不能为空")
    @Size(max = 128, message = "账单文件摘要不能超过 128 个字符")
    @Schema(description = "账单文件摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileDigest;

    @Valid
    @NotEmpty(message = "账单明细不能为空")
    @Schema(description = "账单明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<BillItem> items;

    @Data
    @Schema(description = "通道账单明细导入项")
    public static class BillItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank(message = "通道交易号不能为空")
        @Size(max = 128, message = "通道交易号不能超过 128 个字符")
        @Schema(description = "通道交易号", requiredMode = Schema.RequiredMode.REQUIRED)
        private String channelTradeNo;

        @NotBlank(message = "交易类型不能为空")
        @Size(max = 32, message = "交易类型不能超过 32 个字符")
        @Schema(description = "交易类型：PAYMENT、REFUND、FEE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tradeType;

        @NotNull(message = "金额不能为空")
        @PositiveOrZero(message = "金额不能为负数")
        @Schema(description = "金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long amount;

        @NotNull(message = "手续费不能为空")
        @PositiveOrZero(message = "手续费不能为负数")
        @Schema(description = "手续费，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long fee;

        @NotNull(message = "通道交易时间不能为空")
        @Schema(description = "通道交易时间", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime tradeTime;
    }
}
