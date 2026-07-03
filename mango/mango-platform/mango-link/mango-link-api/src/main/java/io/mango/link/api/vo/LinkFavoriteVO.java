package io.mango.link.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 收藏网址返回对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "收藏网址返回对象")
public class LinkFavoriteVO extends LinkNavigationItemVO {

    private Long favoriteId;
    private LocalDateTime favoriteTime;
}
