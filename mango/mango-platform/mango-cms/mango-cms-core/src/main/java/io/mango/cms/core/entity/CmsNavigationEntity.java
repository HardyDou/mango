package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_navigation")
public class CmsNavigationEntity extends CmsBaseTenantEntity {
    private Long siteId;
    private String navType;
    private String navName;
    private String jumpType;
    private Long categoryId;
    private Long contentId;
    private String externalUrl;
    private String openTarget;
    private Integer sort;
    private String status;
}
