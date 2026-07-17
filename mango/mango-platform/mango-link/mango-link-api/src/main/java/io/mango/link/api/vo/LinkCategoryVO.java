package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkCategoryScope;
import io.mango.link.api.enums.LinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网址分类返回对象。
 */
@Data
@Schema(description = "网址分类返回对象")
public class LinkCategoryVO {

    @Schema(description = "分类 ID")
    private Long id;
    @Schema(description = "分类名称")
    private String name;
    @Schema(description = "分类范围")
    private LinkCategoryScope scope;
    @Schema(description = "归属用户 ID")
    private Long ownerUserId;
    @Schema(description = "归属用户显示名称")
    private String ownerDisplayName;
    @Schema(description = "排序号")
    private Integer sortNo;
    @Schema(description = "分类状态")
    private LinkStatus status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
