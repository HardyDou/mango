package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsAdvertisementVO {
    private Long id;
    private Long siteId;
    private String adCode;
    private String adName;
    private String position;
    private String positionType;
    private String supportedMaterialTypes;
    private Integer width;
    private Integer height;
    private String remark;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
