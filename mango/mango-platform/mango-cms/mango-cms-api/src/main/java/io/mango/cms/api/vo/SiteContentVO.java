package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteContentVO {
    private Long id;
    private String title;
    private String subtitle;
    private String summary;
    private String contentType;
    private String coverFileId;
    private String coverUrl;
    private String body;
    private String externalUrl;
    private String attachmentFileId;
    private String attachmentUrl;
    private String videoFileId;
    private String videoUrl;
    private String source;
    private String author;
    private Long categoryId;
    private String categoryName;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private LocalDateTime publishTime;
}
