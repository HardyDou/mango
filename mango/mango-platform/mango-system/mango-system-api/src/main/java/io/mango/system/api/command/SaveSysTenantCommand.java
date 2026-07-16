package io.mango.system.api.command;

import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存机构命令")
public class SaveSysTenantCommand {
    @Positive(message = "主键 ID必须大于 0")
    @Schema(description = "主键 ID")
    private Long id;
    @NotBlank(message = "tenantName不能为空")
    @Size(max = 100, message = "tenantName长度不能超过100")
    @Schema(description = "租户名称")
    private String tenantName;
    @NotBlank(message = "tenantCode不能为空")
    @Size(max = 50, message = "tenantCode长度不能超过50")
    @Schema(description = "租户编码")
    private String tenantCode;
    @Size(max = 32, message = "机构类型最多32个字符")
    @Schema(description = "机构类型")
    private String institutionType;
    @NotNull(message = "packageId不能为空")
    @Schema(description = "菜单套餐 ID")
    private Long packageId;
    @Size(max = 500, message = "机构能力编码最多500个字符")
    @Schema(description = "能力编码")
    private String capabilityCodes;
    @NotNull(message = "status不能为空")
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人长度不正确")
    private String contact;
    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不正确")
    private String mobile;
    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱长度不正确")
    private String email;
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不正确")
    private String remark;
}
