package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("cms_content")
public class CmsContentEntity extends CmsBaseTenantEntity {
    private String title;
    private String subtitle;
    private String summary;
    private String contentType;
    private String coverFileId;
    private String body;
    private String externalUrl;
    private String attachmentFileId;
    private String videoFileId;
    private String source;
    private String author;
    private Long categoryId;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime offlineTime;
    private String reviewComment;
}
