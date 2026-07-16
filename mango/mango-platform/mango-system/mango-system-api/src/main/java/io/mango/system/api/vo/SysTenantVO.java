package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SysTenantVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户名称")
    private String tenantName;
    @Schema(description = "租户编码")
    private String tenantCode;
    @Schema(description = "机构类型")
    private String institutionType;
    @Schema(description = "能力编码")
    private String capabilityCodes;
    @Schema(description = "菜单套餐 ID")
    private Long packageId;
    @Schema(description = "状态")
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
