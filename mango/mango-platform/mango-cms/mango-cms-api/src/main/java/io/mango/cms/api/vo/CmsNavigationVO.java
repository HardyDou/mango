package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsNavigationVO {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
