package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class CmsSiteSettingVO {
    private Long id;
    private Long siteId;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String footerCopyright;
    private String icpRecord;
    private String contactInfo;
}
