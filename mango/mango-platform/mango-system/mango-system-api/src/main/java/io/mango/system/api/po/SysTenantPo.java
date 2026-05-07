package io.mango.system.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "租户")
public class SysTenantPo {
    @Schema(description = "租户ID")
    private Long id;

    @Schema(description = "租户名称")
    @NotBlank(message = "tenantName不能为空")
    @Size(max = 100, message = "tenantName长度不能超过100")
    private String tenantName;

    @Schema(description = "租户编码")
    @NotBlank(message = "tenantCode不能为空")
    @Size(max = 50, message = "tenantCode长度不能超过50")
    private String tenantCode;

    @Schema(description = "状态：0-禁用，1-启用")
    @NotNull(message = "status不能为空")
    private Integer status;

    @Schema(description = "联系人")
    private String contact;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "备注")
    private String remark;
}
