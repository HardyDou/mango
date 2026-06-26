package io.mango.cms.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsContentPublishVO {
    private Long id;
    private Long contentId;
    private String contentTitle;
    private Long siteId;
    private String siteName;
    private Long categoryId;
    private String categoryName;
    private String publishStatus;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private LocalDateTime offlineTime;
    private Boolean top;
    private String topScope;
    private Boolean recommended;
    private String recommendationType;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
