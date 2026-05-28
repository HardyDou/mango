package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeSendStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_send_record")
public class NoticeSendRecordEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long recipientId;

    private String bizType;

    private String bizId;

    private Long businessChannelTemplateId;

    private Integer templateVersion;

    private NoticeChannelType channelType;

    private Long channelConfigId;

    private String requestId;

    private String providerMessageId;

    private NoticeSendStatus status;

    private String renderedTitle;

    private String renderedContent;

    private String requestSnapshot;

    private String responseSnapshot;

    private String failCode;

    private String failReason;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime sentAt;

    private String tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
