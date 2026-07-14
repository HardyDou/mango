package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_channel_config", excludeProperty = "orgId")
public class NoticeChannelConfigEntity extends NoticeBaseEntity {

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

}
