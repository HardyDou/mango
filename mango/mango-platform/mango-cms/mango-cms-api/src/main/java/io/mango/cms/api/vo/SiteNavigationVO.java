package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class SiteNavigationVO {
    private Long id;
    private String navType;
    private String navName;
    private String jumpType;
    private Long categoryId;
    private Long contentId;
    private String externalUrl;
    private String openTarget;
    private Integer sort;
}
