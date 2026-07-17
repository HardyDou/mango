package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台网址返回对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台网址返回对象")
public class LinkItemVO extends LinkNavigationItemVO {

    @Schema(description = "可见范围")
    private LinkVisibilityScope visibilityScope;
    @Schema(description = "归属用户 ID")
    private Long ownerUserId;
    @Schema(description = "归属用户显示名称")
    private String ownerDisplayName;
    @Schema(description = "可见目标")
    private List<LinkVisibilityTargetVO> visibilityTargets;
    @Schema(description = "网址状态")
    private LinkStatus status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
