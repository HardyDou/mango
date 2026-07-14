package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_site_message_action_request", excludeProperty = {"orgId", "createdBy", "updatedBy"})
public class NoticeSiteMessageActionRequestEntity extends NoticeBaseEntity {

    private Long messageId;

    private Long actionId;

    private String actionCode;

    private Long actorUserId;

    private String requestId;

    private String inputJson;

    private NoticeSiteMessageActionRequestStatus status;

    private String failCode;

    private String failReason;

    private String resultJson;

    private String eventId;

    private LocalDateTime finishedAt;
}
