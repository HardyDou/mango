package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("cms_banner")
public class CmsBannerEntity extends CmsBaseTenantEntity {
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
}
