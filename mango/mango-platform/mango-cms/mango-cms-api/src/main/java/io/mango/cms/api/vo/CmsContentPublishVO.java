package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsContentPublishVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "内容 ID")
    private Long contentId;
    @Schema(description = "内容标题")
    private String contentTitle;
    @Schema(description = "站点 ID")
    private Long siteId;
    @Schema(description = "站点名称")
    private String siteName;
    @Schema(description = "分类 ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "发布状态")
    private String publishStatus;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "计划发布时间")
    private LocalDateTime scheduledPublishTime;
    @Schema(description = "下线时间")
    private LocalDateTime offlineTime;
    @Schema(description = "是否置顶")
    private Boolean top;
    @Schema(description = "置顶范围")
    private String topScope;
    @Schema(description = "是否推荐")
    private Boolean recommended;
    @Schema(description = "推荐类型")
    private String recommendationType;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
