package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "保存支付方式路由规则命令")
public class SavePaymentMethodRouteRuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由规则 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "路由规则编码不能为空")
    @Size(max = 64, message = "路由规则编码不能超过64个字符")
    @Schema(description = "路由规则编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleCode;

    @NotBlank(message = "路由规则名称不能为空")
    @Size(max = 128, message = "路由规则名称不能超过128个字符")
    @Schema(description = "路由规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    @Schema(description = "应用 ID。为空表示当前租户通用")
    private Long appId;

    @Schema(description = "企业主体 ID。为空表示不限制主体")
    private Long subjectId;

    @NotBlank(message = "标准支付方式不能为空")
    @Size(max = 64, message = "标准支付方式不能超过64个字符")
    @Schema(description = "标准支付方式编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodCode;

    @NotBlank(message = "终端类型不能为空")
    @Size(max = 32, message = "终端类型不能超过32个字符")
    @Schema(description = "终端类型：WEB/H5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalType;

    @NotBlank(message = "接入场景不能为空")
    @Size(max = 32, message = "接入场景不能超过32个字符")
    @Schema(description = "接入场景，例如 MANGO_PAY/PROD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String environment;

    @NotBlank(message = "路由模式不能为空")
    @Size(max = 32, message = "路由模式不能超过32个字符")
    @Schema(description = "路由模式：PRIORITY/MANUAL/WEIGHT/COST/HEALTH", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routeMode;

    @NotNull(message = "失败降级开关不能为空")
    @Schema(description = "是否允许失败降级：1-允许，0-不允许", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer fallbackEnabled;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Valid
    @NotEmpty(message = "路由明细不能为空")
    @Schema(description = "路由明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SavePaymentMethodRouteRuleItemCommand> items;
}
