package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeTaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_task", excludeProperty = "orgId")
public class NoticeTaskEntity extends NoticeBaseEntity {

    private String taskCode;

    private String bizType;

    private String bizId;

    private String idempotentKey;

    private String paramsSnapshot;

    private String recipientTargetsSnapshot;

    private String channelTypes;

    private String messageScene;

    private String messageSubjectType;

    private String messageSubjectId;

    private String messageSubjectName;

    private String messageTargetType;

    private String messageTargetKey;

    private String messageTargetParamsJson;

    private String messageTargetOpenMode;

    private String messageDataJson;

    private String messageActionsJson;

    private LocalDateTime messageExpireTime;

    private NoticeSendMode sendMode;

    private LocalDateTime scheduledTime;

    private NoticeTaskStatus status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

}
