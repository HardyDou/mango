package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsSiteCategoryVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点 ID")
    private Long siteId;
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
    @Schema(description = "显示状态")
    private String visibleStatus;
    @Schema(description = "访问类型")
    private String accessType;
    @Schema(description = "角色编码列表")
    private String roleCodes;
    @Schema(description = "SEO 标题")
    private String seoTitle;
    @Schema(description = "SEO 关键词")
    private String seoKeywords;
    @Schema(description = "SEO 描述")
    private String seoDescription;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @Schema(description = "子项列表")
    private List<CmsSiteCategoryVO> children = new ArrayList<>();
}
