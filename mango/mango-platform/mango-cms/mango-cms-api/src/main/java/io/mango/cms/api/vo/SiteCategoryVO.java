package io.mango.cms.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteCategoryVO {
    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private String categoryType;
    private String accessPath;
    private String externalUrl;
    private Integer sort;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private List<SiteCategoryVO> children = new ArrayList<>();
}
