package io.mango.link.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页网址导航小组件数据。
 */
@Data
@Schema(description = "首页网址导航小组件数据")
public class LinkNavigationWidgetDataVO {

    @Schema(description = "企业网址")
    private List<LinkNavigationItemVO> companyItems = List.of();

    @Schema(description = "个人网址")
    private List<LinkPersonalItemVO> personalItems = List.of();

    @Schema(description = "我的收藏")
    private List<LinkFavoriteVO> favoriteItems = List.of();

    @Schema(description = "个人分组")
    private List<LinkCategoryVO> categories = List.of();
}
