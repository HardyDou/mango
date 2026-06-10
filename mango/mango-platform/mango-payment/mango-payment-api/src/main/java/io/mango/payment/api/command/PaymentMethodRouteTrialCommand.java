package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "支付方式路由试算命令")
public class PaymentMethodRouteTrialCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "应用不能为空")
    @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long applicationId;

    @NotNull(message = "企业主体不能为空")
    @Schema(description = "企业主体 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long subjectId;

    @NotBlank(message = "标准支付方式不能为空")
    @Schema(description = "标准支付方式编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodCode;

    @NotBlank(message = "终端类型不能为空")
    @Schema(description = "终端类型：WEB/H5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalType;

    @NotBlank(message = "接入场景不能为空")
    @Schema(description = "接入场景", requiredMode = Schema.RequiredMode.REQUIRED)
    private String environment;

    @NotNull(message = "金额不能为空")
    @Schema(description = "金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;
}
