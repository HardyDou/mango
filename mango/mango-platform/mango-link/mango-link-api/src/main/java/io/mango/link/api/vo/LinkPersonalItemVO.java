package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkOpenMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人网址返回对象。
 */
@Data
@Schema(description = "个人网址返回对象")
public class LinkPersonalItemVO {

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
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "打开方式")
    private LinkOpenMode openMode;
    @Schema(description = "是否已收藏")
    private Boolean favorited;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
