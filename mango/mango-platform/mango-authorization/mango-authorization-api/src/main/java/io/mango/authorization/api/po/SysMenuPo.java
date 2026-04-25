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
 * System menu PO for add/update requests
 * Contains validation annotations for request validation
 *
 * @author Mango
 */
@Data
@Schema(description = "菜单添加/修改请求")
public class SysMenuPo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Menu ID (null for add, required for update)
     */
    @Schema(description = "菜单ID (更新时必填)")
    private Long menuId;

    /**
     * Menu group ID
     */
    @NotNull(message = "菜单分组ID不能为空")
    @Min(value = 1, message = "菜单分组ID必须大于0")
    @Schema(description = "菜单分组ID")
    private Long groupId;

    /**
     * Parent menu ID (0: root)
     */
    @NotNull(message = "父菜单ID不能为空")
    @Min(value = 0, message = "父菜单ID必须大于等于0")
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * Menu type (1: directory, 2: menu, 3: button)
     */
    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型最小值为1")
    @Max(value = 3, message = "菜单类型最大值为3")
    @Schema(description = "菜单类型: 1-目录, 2-菜单, 3-按钮")
    private Integer menuType;

    /**
     * Menu name
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称最多50个字符")
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * Menu code/permission (e.g., system:user:view)
     */
    @NotBlank(message = "菜单权限标识不能为空")
    @Size(max = 100, message = "菜单权限标识最多100个字符")
    @Schema(description = "菜单权限标识")
    private String menuCode;

    /**
     * Path (for menu type)
     */
    @Size(max = 255, message = "路由路径最多255个字符")
    @Schema(description = "前端路由路径")
    private String path;

    /**
     * Icon
     */
    @Size(max = 100, message = "菜单图标最多100个字符")
    @Schema(description = "菜单图标")
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
     * Visible (0: hidden, 1: visible)
     */
    @Min(value = 0, message = "是否显示最小值为0")
    @Max(value = 1, message = "是否显示最大值为1")
    @Schema(description = "是否显示: 0-隐藏, 1-显示")
    private Integer visible;

    /**
     * Frontend component path (e.g., @/views/system/user/index.vue)
     */
    @Size(max = 255, message = "前端组件路径最多255个字符")
    @Schema(description = "前端组件路径")
    private String component;

    /**
     * Route cache (0: no cache, 1: cache)
     */
    @Min(value = 0, message = "路由缓存最小值为0")
    @Max(value = 1, message = "路由缓存最大值为1")
    @Schema(description = "路由缓存: 0-不缓存, 1-缓存")
    private Integer keepAlive;

    /**
     * Embedded mode (0: no, 1: iframe embed)
     */
    @Min(value = 0, message = "内嵌模式最小值为0")
    @Max(value = 1, message = "内嵌模式最大值为1")
    @Schema(description = "内嵌模式: 0-否, 1-iframe内嵌")
    private Integer embedded;

    /**
     * Redirect path
     */
    @Size(max = 255, message = "重定向路径最多255个字符")
    @Schema(description = "重定向路径")
    private String redirect;

    /**
     * Permission identifiers (e.g., system:user:add,system:user:edit)
     */
    @Size(max = 500, message = "权限标识列表最多500个字符")
    @Schema(description = "权限标识列表")
    private String permissions;

    /**
     * Remark
     */
    @Size(max = 500, message = "备注最多500个字符")
    @Schema(description = "备注")
    private String remark;
}
