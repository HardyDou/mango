package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_site")
public class CmsSiteEntity extends CmsBaseTenantEntity {
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
}
