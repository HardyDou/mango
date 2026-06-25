package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("cms_ad_delivery")
public class CmsAdDeliveryEntity extends CmsBaseTenantEntity {
    private Long siteId;
    private Long adId;
    private String deliveryName;
    private String materialType;
    private String title;
    private String textContent;
    private String richContent;
    private String htmlContent;
    private String imageFileId;
    private String imageFileIds;
    private String videoFileId;
    private String coverFileId;
    private String jumpUrl;
    private String openTarget;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sort;
    private String status;
}
