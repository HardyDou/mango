package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System menu entity
 *
 * @author Mango
 */
@Data
@TableName("sys_menu")
@Schema(description = "菜单")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Menu ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "菜单ID")
    private Long menuId;

    /**
     * Tenant ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * Menu group ID
     */
    @Schema(description = "菜单分组ID")
    private Long groupId;

    /**
     * Parent menu ID (0: root)
     */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * Menu type (1: directory, 2: menu, 3: button)
     */
    @Schema(description = "菜单类型: 1-目录, 2-菜单, 3-按钮")
    private Integer menuType;

    /**
     * Menu name
     */
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * Menu code/permission (e.g., system:user:view)
     */
    @Schema(description = "菜单权限标识")
    private String menuCode;

    /**
     * Path (for menu type)
     */
    @Schema(description = "前端路由路径")
    private String path;

    /**
     * Icon
     */
    @Schema(description = "菜单图标")
    private String icon;

    /**
     * Sort order
     */
    @Schema(description = "排序号")
    private Integer sort;

    /**
     * Status (0: disabled, 1: enabled)
     */
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    /**
     * Visible (0: hidden, 1: visible)
     */
    @Schema(description = "是否显示: 0-隐藏, 1-显示")
    private Integer visible;

    /**
     * Frontend component path (e.g., @/views/system/user/index.vue)
     */
    @Schema(description = "前端组件路径")
    private String component;

    /**
     * Route cache (0: no cache, 1: cache)
     */
    @Schema(description = "路由缓存: 0-不缓存, 1-缓存")
    private Integer keepAlive;

    /**
     * Embedded mode (0: no, 1: iframe embed)
     */
    @Schema(description = "内嵌模式: 0-否, 1-iframe内嵌")
    private Integer embedded;

    /**
     * Redirect path
     */
    @Schema(description = "重定向路径")
    private String redirect;

    /**
     * Permission identifiers (e.g., system:user:add,system:user:edit)
     */
    @Schema(description = "权限标识列表")
    private String permissions;

    /**
     * Creator
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;

    /**
     * Updater
     */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改人")
    private String updateBy;

    /**
     * Create time
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * Remark
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * Delete flag (0: normal, 1: deleted)
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "删除标记: 0-正常, 1-已删除")
    private Integer delFlag;
}
