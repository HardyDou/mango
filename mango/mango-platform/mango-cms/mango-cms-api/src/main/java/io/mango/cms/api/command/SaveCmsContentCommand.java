package io.mango.cms.api.command;

import io.mango.cms.api.validation.CmsStrictValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaveCmsContentCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    @Schema(description = "标题")
    private String title;

    @Size(max = 255, message = "副标题最多255个字符")
    @Schema(description = "副标题")
    private String subtitle;

    @Size(max = 1024, message = "摘要最多1024个字符")
    @Schema(description = "摘要")
    private String summary;

    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "ARTICLE|IMAGE_TEXT|PAGE|ATTACHMENT|VIDEO", message = "内容类型不合法")
    @Schema(description = "内容类型")
    private String contentType;

    @Size(max = 128, message = "封面文件 ID 最多128个字符")
    @Schema(description = "封面文件 ID")
    private String coverFileId;

    @Schema(description = "内容正文")
    @Size(max = 65535, message = "内容正文最多65535个字符")
    private String body;

    @Size(max = 512, message = "外部地址最多512个字符")
    @Schema(description = "外部地址")
    private String externalUrl;

    @Size(max = 128, message = "附件文件 ID 最多128个字符")
    @Schema(description = "附件文件 ID")
    private String attachmentFileId;

    @Size(max = 128, message = "视频文件 ID 最多128个字符")
    @Schema(description = "视频文件 ID")
    private String videoFileId;

    @Size(max = 128, message = "来源最多128个字符")
    @Schema(description = "来源")
    private String source;

    @Size(max = 128, message = "作者最多128个字符")
    @Schema(description = "作者")
    private String author;

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    @Size(max = 50, message = "标签最多50个")
    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    @Schema(description = "SEO 标题")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    @Schema(description = "SEO 描述")
    private String seoDescription;

    @Schema(description = "发布时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下发布时间不能为空")
    private LocalDateTime publishTime;

    @Schema(description = "下线时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下下线时间不能为空")
    private LocalDateTime offlineTime;
}
