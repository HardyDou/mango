package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsContentVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "副标题")
    private String subtitle;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "内容类型")
    private String contentType;
    @Schema(description = "封面文件 ID")
    private String coverFileId;
    @Schema(description = "内容正文")
    private String body;
    @Schema(description = "外部地址")
    private String externalUrl;
    @Schema(description = "附件文件 ID")
    private String attachmentFileId;
    @Schema(description = "视频文件 ID")
    private String videoFileId;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "分类 ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "标签列表")
    private List<CmsContentTagVO> tags = new ArrayList<>();
    @Schema(description = "SEO 标题")
    private String seoTitle;
    @Schema(description = "SEO 关键词")
    private String seoKeywords;
    @Schema(description = "SEO 描述")
    private String seoDescription;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "下线时间")
    private LocalDateTime offlineTime;
    @Schema(description = "审核意见")
    private String reviewComment;
    @Schema(description = "组织 ID")
    private Long orgId;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
