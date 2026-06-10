package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存支付通道能力命令")
public class SavePaymentChannelCapabilityCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通道能力 ID。新增时为空")
    private Long id;

    @NotBlank(message = "标准支付方式编码不能为空")
    @Schema(description = "标准支付方式编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodCode;

    @NotBlank(message = "终端类型不能为空")
    @Schema(description = "终端类型，例如 WEB/H5/APP/MP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalType;

    @NotBlank(message = "接入场景不能为空")
    @Schema(description = "接入场景，例如 MANGO_PAY/PROD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String environment;

    @Schema(description = "是否支持退款：1-支持，0-不支持")
    private Integer supportsRefund;

    @Schema(description = "是否支持查单：1-支持，0-不支持")
    private Integer supportsQuery;

    @Schema(description = "是否支持关单：1-支持，0-不支持")
    private Integer supportsClose;

    @Schema(description = "是否支持账单：1-支持，0-不支持")
    private Integer supportsBill;

    @Schema(description = "是否支持对账：1-支持，0-不支持")
    private Integer supportsReconcile;

    @Schema(description = "最小金额，单位分")
    private Long minAmount;

    @Schema(description = "最大金额，单位分")
    private Long maxAmount;

    @NotNull(message = "通道能力状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
