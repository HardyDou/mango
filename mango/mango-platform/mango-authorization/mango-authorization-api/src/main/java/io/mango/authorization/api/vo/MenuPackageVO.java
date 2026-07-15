package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单授权套餐。
 */
@Data
@Schema(description = "菜单授权套餐")
public class MenuPackageVO {

    @Schema(description = "套餐ID")
    private Long packageId;
    @Schema(description = "套餐名称")
    private String packageName;
    @Schema(description = "套餐编码")
    private String packageCode;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "套餐包含的菜单ID集合")
    private List<Long> menuIds;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
