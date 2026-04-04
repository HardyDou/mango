package io.mango.permission.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System menu group PO (API layer - no DB annotations)
 * Used for internal data transfer, not for MyBatis-Plus operations
 *
 * @author Mango
 */
@Data
@Schema(description = "菜单分组")
public class SysMenuGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Group ID
     */
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
    @Schema(description = "创建人")
    private String createBy;

    /**
     * Updater
     */
    @Schema(description = "修改人")
    private String updateBy;

    /**
     * Create time
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * Update time
     */
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
    @Schema(description = "删除标记: 0-正常, 1-已删除")
    private Integer delFlag;
}
