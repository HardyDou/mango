package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_site_category")
public class CmsSiteCategoryEntity extends CmsBaseTenantEntity {
    private Long siteId;
    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private String categoryType;
    private String accessPath;
    private String externalUrl;
    private Integer sort;
    private String visibleStatus;
    private String accessType;
    private String roleCodes;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
}
