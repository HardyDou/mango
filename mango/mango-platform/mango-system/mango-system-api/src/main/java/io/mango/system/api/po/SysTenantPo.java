package io.mango.system.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "机构")
public class SysTenantPo {
    @Schema(description = "机构ID。底层对应 tenantId，用于机构空间隔离")
    private Long id;

    @Schema(description = "机构名称")
    @jakarta.validation.constraints.NotBlank(message = "tenantName不能为空")
    @Size(max = 100, message = "tenantName长度不能超过100")
    private String tenantName;

    @Schema(description = "机构编码")
    @jakarta.validation.constraints.NotBlank(message = "tenantCode不能为空")
    @Size(max = 50, message = "tenantCode长度不能超过50")
    private String tenantCode;

    @Schema(description = "机构类型。字典 institution_type；为空时默认 ENTERPRISE")
    @Size(max = 32, message = "机构类型最多32个字符")
    private String institutionType;

    @Schema(description = "机构能力编码，多个用逗号分隔。字典 institution_capability")
    @Size(max = 500, message = "机构能力编码最多500个字符")
    private String capabilityCodes;

    @Schema(description = "机构生命周期状态。字典 institution_status：0-禁用，1-启用，2-冻结，9-归档")
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
