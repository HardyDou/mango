package io.mango.authorization.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * System menu group PO for add/update requests
 * Contains validation annotations for request validation
 *
 * @author Mango
 */
@Data
@Schema(description = "菜单分组添加/修改请求")
public class SysMenuGroupPo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Group ID (null for add, required for update)
     */
    @Schema(description = "分组ID (更新时必填)")
    private Long groupId;

    /**
     * Group name
     */
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称最多50个字符")
    @Schema(description = "分组名称")
    private String groupName;

    /**
     * Group code (unique identifier)
     */
    @NotBlank(message = "分组编码不能为空")
    @Size(max = 50, message = "分组编码最多50个字符")
    @Schema(description = "分组编码")
    private String groupCode;

    /**
     * Group icon
     */
    @Size(max = 100, message = "分组图标最多100个字符")
    @Schema(description = "分组图标")
    private String icon;

    /**
     * Sort order
     */
    @Min(value = 0, message = "排序号最小值为0")
    @Schema(description = "排序号")
    private Integer sort;

    /**
     * Status (0: disabled, 1: enabled)
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    /**
     * Remark
     */
    @Size(max = 500, message = "备注最多500个字符")
    @Schema(description = "备注")
    private String remark;
}
