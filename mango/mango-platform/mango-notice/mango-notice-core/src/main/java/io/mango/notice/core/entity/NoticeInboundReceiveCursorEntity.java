package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_inbound_receive_cursor", excludeProperty = "orgId")
public class NoticeInboundReceiveCursorEntity extends NoticeBaseEntity {
    private Long channelConfigId;
    private NoticeInboundProtocol protocol;
    private String cursorValue;
    private String cursorVersion;
    private LocalDateTime lastPolledAt;
    private LocalDateTime nextPollAt;
    private String lastFailureCode;
    private String lastFailureReason;
}
