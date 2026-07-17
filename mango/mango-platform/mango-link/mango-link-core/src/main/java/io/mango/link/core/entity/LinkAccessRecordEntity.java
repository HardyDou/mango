package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("link_access_record")
public class LinkAccessRecordEntity extends TenantEntity {

    private Long linkId;

    private String url;

    private Long userId;

    private String visitorId;

    private String source;

    private String extraParams;

    private String clientIp;

    private String userAgent;

    private String referer;

    private LocalDateTime accessTime;

}
