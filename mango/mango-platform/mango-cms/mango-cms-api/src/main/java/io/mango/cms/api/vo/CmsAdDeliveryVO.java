package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsAdDeliveryVO {
    private Long id;
    private Long siteId;
    private Long adId;
    private String adName;
    private String adCode;
    private String position;
    private String positionType;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
