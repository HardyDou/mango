package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class SiteAdvertisementVO {
    private Long id;
    private String adCode;
    private String adName;
    private String position;
    private String positionType;
    private String adType;
    private String materialType;
    private String materialFileId;
    private String title;
    private String textContent;
    private String richContent;
    private String htmlContent;
    private String imageFileId;
    private String imageFileIds;
    private String imageUrl;
    private String imageUrls;
    private String videoFileId;
    private String coverFileId;
    private String videoUrl;
    private String coverUrl;
    private String jumpUrl;
    private String openTarget;
    private Integer sort;
}
