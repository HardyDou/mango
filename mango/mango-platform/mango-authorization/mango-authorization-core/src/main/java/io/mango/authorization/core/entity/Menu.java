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
 * 菜单实体。
 */
@Data
@TableName("authorization_menu")
@Schema(description = "菜单")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "菜单ID")
    private Long menuId;

    /** 租户 ID。 */
    @Schema(description = "租户ID")
    private Long tenantId;

    /** 应用编码。 */
    @Schema(description = "应用编码")
    private String appCode;

    /** 父菜单 ID，0 表示根节点。 */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /** 菜单类型：1-目录，2-菜单，3-按钮。 */
    @Schema(description = "菜单类型: 1-目录, 2-菜单, 3-按钮")
    private Integer menuType;

    /** 菜单名称。 */
    @Schema(description = "菜单名称")
    private String menuName;

    /** 菜单权限标识。 */
    @Schema(description = "菜单权限标识")
    private String menuCode;

    /** 前端路由路径。 */
    @Schema(description = "前端路由路径")
    private String path;

    /** 菜单图标。 */
    @Schema(description = "菜单图标")
    private String icon;

    /** 排序号。 */
    @Schema(description = "排序号")
    private Integer sort;

    /** 状态：0-禁用，1-启用。 */
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    /** 是否显示：0-隐藏，1-显示。 */
    @Schema(description = "是否显示: 0-隐藏, 1-显示")
    private Integer visible;

    /** 前端组件路径。 */
    @Schema(description = "前端组件路径")
    private String component;

    /** 路由缓存：0-不缓存，1-缓存。 */
    @Schema(description = "路由缓存: 0-不缓存, 1-缓存")
    private Integer keepAlive;

    /** 内嵌模式：0-否，1-iframe 内嵌。 */
    @Schema(description = "内嵌模式: 0-否, 1-iframe内嵌")
    private Integer embedded;

    /** 重定向路径。 */
    @Schema(description = "重定向路径")
    private String redirect;

    /** 权限标识列表。 */
    @Schema(description = "权限标识列表")
    private String permissions;

    /** 创建人。 */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;

    /** 修改人。 */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改人")
    private String updateBy;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 备注。 */
    @Schema(description = "备注")
    private String remark;

    /** 删除标记：0-正常，1-已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "删除标记: 0-正常, 1-已删除")
    private Integer delFlag;
}
