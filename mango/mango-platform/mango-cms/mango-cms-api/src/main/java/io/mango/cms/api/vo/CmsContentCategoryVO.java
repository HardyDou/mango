package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsContentCategoryVO {
    private Long id;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer sort;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CmsContentCategoryVO> children = new ArrayList<>();
}
