package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsContentVO {
    private Long id;
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
    private String categoryName;
    private List<CmsContentTagVO> tags = new ArrayList<>();
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime offlineTime;
    private String reviewComment;
    private Long orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
