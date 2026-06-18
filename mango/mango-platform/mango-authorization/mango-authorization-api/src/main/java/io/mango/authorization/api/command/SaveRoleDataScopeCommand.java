package io.mango.authorization.api.command;

import io.mango.authorization.api.enums.DataScopeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存角色数据权限命令。
 */
@Data
@Schema(description = "保存角色数据权限命令")
public class SaveRoleDataScopeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色 ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "资源编码")
    @NotBlank(message = "资源编码不能为空")
    @Size(max = 128, message = "资源编码最多128个字符")
    private String resourceCode;

    @Schema(description = "数据范围模式")
    @NotNull(message = "数据范围模式不能为空")
    private DataScopeMode scopeMode;

    @Schema(description = "范围值，ORG 模式下为组织 ID 列表")
    private List<String> scopeValues = new ArrayList<>();

    @Schema(description = "是否包含下级组织，ORG 模式作用于所选组织，SELF_ORG_AND_CHILDREN 模式固定包含当前主体主组织下级")
    private Boolean includeChildren;

    @Schema(description = "状态：0-禁用，1-启用")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    private Integer status;
}
