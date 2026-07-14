package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_site_message_action", excludeProperty = {"orgId", "createdBy", "updatedBy"})
public class NoticeSiteMessageActionEntity extends NoticeBaseEntity {

    private Long messageId;

    private String actionCode;

    private String actionLabel;

    private NoticeSiteMessageActionInteractionType interactionType;

    private String eventType;

    private NoticeSiteMessageTargetType targetType;

    private String targetKey;

    private String targetParamsJson;

    private String targetOpenMode;

    private Boolean confirmRequired;

    private String inputSchema;

    private NoticeSiteMessageActionStatus status;

    private String failureReason;

    private Integer sortOrder;

    private LocalDateTime expireTime;

}
