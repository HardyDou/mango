package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsContentTagVO {
    private Long id;
    private String tagCode;
    private String tagName;
    private Integer sort;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
