package io.mango.permission.api.vo;

import io.mango.common.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System menu group VO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单分组VO")
public class SysMenuGroupVO extends BaseVO {

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
     * Group code
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
     * Child menus
     */
    @Schema(description = "子菜单")
    private List<SysMenuVO> children;

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
}
