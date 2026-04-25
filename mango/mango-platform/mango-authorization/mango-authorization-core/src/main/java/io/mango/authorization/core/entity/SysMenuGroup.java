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
 * System menu group entity
 * 菜单分组，用于对菜单进行一级分类
 *
 * @author Mango
 */
@Data
@TableName("authorization_menu_group")
@Schema(description = "菜单分组")
public class SysMenuGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Group ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "分组ID")
    private Long groupId;

    /**
     * Group name
     */
    @Schema(description = "分组名称")
    private String groupName;

    /**
     * Group code (unique identifier)
     */
    @Schema(description = "分组编码")
    private String groupCode;

    /**
     * Group icon
     */
    @Schema(description = "分组图标")
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
