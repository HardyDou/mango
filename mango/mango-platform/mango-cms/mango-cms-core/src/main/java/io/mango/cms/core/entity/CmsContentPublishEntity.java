package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("cms_content_publish")
public class CmsContentPublishEntity extends CmsBaseTenantEntity {
    private Long contentId;
    private Long siteId;
    private Long categoryId;
    private String publishStatus;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private LocalDateTime offlineTime;
    private Boolean top;
    private String topScope;
    private Boolean recommended;
    private String recommendationType;
    private Integer sort;
}
