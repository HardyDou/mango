package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class SiteResolveVO {
    private Long siteId;
    private String siteCode;
    private String siteName;
    private String status;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String footerCopyright;
    private String icpRecord;
    private String contactInfo;
}
