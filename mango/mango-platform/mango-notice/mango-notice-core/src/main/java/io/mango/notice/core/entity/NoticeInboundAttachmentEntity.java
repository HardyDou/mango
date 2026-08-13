package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeInboundAttachmentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_inbound_attachment", excludeProperty = "orgId")
public class NoticeInboundAttachmentEntity extends NoticeBaseEntity {
    private Long messageId;
    private Integer attachmentIndex;
    private Long fileId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String contentSha256;
    private NoticeInboundAttachmentStatus status;
    private String failureCode;
    private String failureReason;
    private Integer attemptCount;
}
