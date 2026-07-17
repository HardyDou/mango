package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkOpenMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户侧网址返回对象。
 */
@Data
@Schema(description = "用户侧网址返回对象")
public class LinkNavigationItemVO {

    @Schema(description = "网址 ID")
    private Long id;
    @Schema(description = "分类 ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "网址名称")
    private String name;
    @Schema(description = "网址地址")
    private String url;
    @Schema(description = "简介")
    private String summary;
    @Schema(description = "图标地址")
    private String iconUrl;
    @Schema(description = "标签")
    private List<String> tags;
    @Schema(description = "打开方式")
    private LinkOpenMode openMode;
    @Schema(description = "是否推荐")
    private Boolean recommended;
    @Schema(description = "排序号")
    private Integer sortNo;
    @Schema(description = "是否已收藏")
    private Boolean favorited;
}
