package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteContentVO {
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
    @Schema(description = "封面地址")
    private String coverUrl;
    @Schema(description = "内容正文")
    private String body;
    @Schema(description = "外部地址")
    private String externalUrl;
    @Schema(description = "附件文件 ID")
    private String attachmentFileId;
    @Schema(description = "附件地址")
    private String attachmentUrl;
    @Schema(description = "视频文件 ID")
    private String videoFileId;
    @Schema(description = "视频地址")
    private String videoUrl;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "分类 ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "SEO 标题")
    private String seoTitle;
    @Schema(description = "SEO 关键词")
    private String seoKeywords;
    @Schema(description = "SEO 描述")
    private String seoDescription;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
