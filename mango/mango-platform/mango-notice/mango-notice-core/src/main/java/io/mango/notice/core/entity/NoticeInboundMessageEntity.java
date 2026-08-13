package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_inbound_message", excludeProperty = "orgId")
public class NoticeInboundMessageEntity extends NoticeBaseEntity {
    private Long channelConfigId;
    private NoticeChannelType channelType;
    private String providerCode;
    private String sourceKey;
    private String messageId;
    private String subject;
    private String fromAddress;
    private String toAddressesJson;
    private String bodyText;
    private String bodyHtml;
    private String rawHeadersJson;
    private NoticeInboundMessageStatus status;
    private String eventId;
    private String failureCode;
    private String failureReason;
    private Integer attemptCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}
