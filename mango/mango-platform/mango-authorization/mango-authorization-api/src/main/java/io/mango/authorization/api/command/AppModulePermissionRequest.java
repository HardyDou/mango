package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "应用模块按钮权限请求")
public class AppModulePermissionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    @Schema(description = "按钮菜单编码")
    private String menuCode;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "权限码")
    private String permissionCode;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "权限名称")
    private String permissionName;
    @Min(0)
    @Schema(description = "排序号")
    private Integer sort;
    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Size(max = 100)
    @Schema(description = "套餐编码列表")
    private List<@Size(max = 64) String> packageCodes;
    @Size(max = 100)
    @Schema(description = "默认角色编码列表")
    private List<@Size(max = 50) String> roleCodes;
    @Size(max = 500)
    @Schema(description = "备注")
    private String remark;
}
