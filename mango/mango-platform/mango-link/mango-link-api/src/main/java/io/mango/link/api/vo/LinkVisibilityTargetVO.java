package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkVisibilityTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网址可见目标返回对象。
 */
@Data
@Schema(description = "网址可见目标返回对象")
public class LinkVisibilityTargetVO {

    @Schema(description = "可见目标记录 ID")
    private Long id;
    @Schema(description = "目标类型")
    private LinkVisibilityTargetType targetType;
    @Schema(description = "目标 ID")
    private Long targetId;
    @Schema(description = "目标名称")
    private String targetName;
}
