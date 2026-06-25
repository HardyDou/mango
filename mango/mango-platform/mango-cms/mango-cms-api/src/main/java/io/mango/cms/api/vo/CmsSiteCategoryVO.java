package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsSiteCategoryVO {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CmsSiteCategoryVO> children = new ArrayList<>();
}
