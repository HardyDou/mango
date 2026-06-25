package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("cms_advertisement")
public class CmsAdvertisementEntity extends CmsBaseTenantEntity {
    private Long siteId;
    private String adCode;
    private String adName;
    private String position;
    private String positionType;
    private String supportedMaterialTypes;
    private Integer width;
    private Integer height;
    private String remark;
    private String adType;
    private String materialFileId;
    private String jumpUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sort;
    private String status;
}
