package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsSiteVO {
    private Long id;
    private String siteName;
    private String siteCode;
    private String logoFileId;
    private String description;
    private String domain;
    private String status;
    private String defaultLanguage;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String footerCopyright;
    private String icpRecord;
    private String contactInfo;
    private Long orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
