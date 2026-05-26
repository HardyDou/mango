package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_channel_config")
public class NoticeChannelConfigEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private NoticeChannelType channelType;

    private String providerCode;

    private String configName;

    private String configJson;

    private Boolean enabled;

    private Integer priority;

    private Integer weight;

    private NoticeChannelConfigStatus configStatus;

    private NoticeChannelSendHealthStatus lastSendStatus;

    private LocalDateTime lastSendTime;

    private String lastFailureCode;

    private String lastFailureReason;

    private String rateLimitConfig;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
