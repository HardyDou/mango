package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_access_record")
public class LinkAccessRecordEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

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

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
