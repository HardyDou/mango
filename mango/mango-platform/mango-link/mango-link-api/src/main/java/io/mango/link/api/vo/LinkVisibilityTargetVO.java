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

    private Long id;
    private LinkVisibilityTargetType targetType;
    private Long targetId;
    private String targetName;
}
