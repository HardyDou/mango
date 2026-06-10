package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存收款主体命令")
public class SavePaymentEnterpriseSubjectCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主体 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "主体名称不能为空")
    @Size(max = 128, message = "主体名称不能超过128个字符")
    @Schema(description = "主体名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subjectName;

    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(max = 64, message = "统一社会信用代码不能超过64个字符")
    @Schema(description = "统一社会信用代码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String creditCode;

    @NotBlank(message = "银行账户不能为空")
    @Size(max = 64, message = "银行账户不能超过64个字符")
    @Schema(description = "银行账户", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankAccountNo;

    @NotBlank(message = "开户行不能为空")
    @Size(max = 128, message = "开户行不能超过128个字符")
    @Schema(description = "开户行", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankName;

    @Schema(description = "证照文件 ID")
    private Long licenseFileId;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
