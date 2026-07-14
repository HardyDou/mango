package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteCategoryVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "父级 ID")
    private Long parentId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "分类编码")
    private String categoryCode;
    @Schema(description = "分类类型")
    private String categoryType;
    @Schema(description = "访问路径")
    private String accessPath;
    @Schema(description = "外部地址")
    private String externalUrl;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "SEO 标题")
    private String seoTitle;
    @Schema(description = "SEO 关键词")
    private String seoKeywords;
    @Schema(description = "SEO 描述")
    private String seoDescription;
    @Schema(description = "子项列表")
    private List<SiteCategoryVO> children = new ArrayList<>();
}
