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

    private LinkVisibilityScope visibilityScope;
    private List<LinkVisibilityTargetVO> visibilityTargets;
    private LinkStatus status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
