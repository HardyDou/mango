package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存收银台配置命令")
public class SavePaymentCashierConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收银台配置 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "收银台名称不能为空")
    @Size(max = 128, message = "收银台名称不能超过128个字符")
    @Schema(description = "收银台名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cashierName;

    @NotNull(message = "应用 ID 不能为空")
    @Schema(description = "适用应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long applicationId;

    @NotNull(message = "默认收银台标记不能为空")
    @Schema(description = "是否默认收银台：1-是，0-否", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer defaultCashier;

    @NotBlank(message = "允许企业主体不能为空")
    @Size(max = 1024, message = "允许企业主体不能超过1024个字符")
    @Schema(description = "允许企业主体 ID，逗号分隔", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseSubjectIds;

    @Schema(description = "可见标准支付方式编码，逗号分隔")
    private String methodCodes;

    @Schema(description = "默认标准支付方式编码")
    private String defaultMethodCode;

    @Schema(description = "支付方式展示顺序")
    private String methodDisplayOrder;

    @Size(max = 512, message = "结果跳转地址不能超过512个字符")
    @Schema(description = "结果跳转地址")
    private String resultReturnUrl;

    @Schema(description = "基础展示主体配置 JSON，包括 logoFileId、subtitle、helpText")
    private String displayConfig;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
