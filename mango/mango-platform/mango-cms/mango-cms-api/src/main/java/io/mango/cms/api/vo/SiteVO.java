package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class SiteVO {
    private Long id;
    private String siteName;
    private String siteCode;
    private String logoFileId;
    private String logoUrl;
    private String description;
    private String domain;
    private String defaultLanguage;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String footerCopyright;
    private String icpRecord;
    private String contactInfo;
}
