package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaveCmsContentCommand {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    private String title;

    @Size(max = 255, message = "副标题最多255个字符")
    private String subtitle;

    @Size(max = 1024, message = "摘要最多1024个字符")
    private String summary;

    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "ARTICLE|IMAGE_TEXT|PAGE|ATTACHMENT|VIDEO", message = "内容类型不合法")
    private String contentType;

    @Size(max = 128, message = "封面文件 ID 最多128个字符")
    private String coverFileId;

    private String body;

    @Size(max = 512, message = "外部地址最多512个字符")
    private String externalUrl;

    @Size(max = 128, message = "附件文件 ID 最多128个字符")
    private String attachmentFileId;

    @Size(max = 128, message = "视频文件 ID 最多128个字符")
    private String videoFileId;

    @Size(max = 128, message = "来源最多128个字符")
    private String source;

    @Size(max = 128, message = "作者最多128个字符")
    private String author;

    private Long categoryId;

    @Size(max = 50, message = "标签最多50个")
    private List<Long> tagIds;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    private String seoDescription;

    private LocalDateTime publishTime;

    private LocalDateTime offlineTime;
}
