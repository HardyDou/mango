package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsBannerVO {
    private Long id;
    private Long siteId;
    private String position;
    private String title;
    private String subtitle;
    private String mediaType;
    private String mediaFileId;
    private String jumpUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
