package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_site_message", excludeProperty = "orgId")
public class NoticeSiteMessageEntity extends NoticeBaseEntity {

    private Long taskId;

    private Long sendRecordId;

    private Long userId;

    private String title;

    private String content;

    private String messageScene;

    private String subjectType;

    private String subjectId;

    private String subjectName;

    private NoticeSiteMessageTargetType targetType;

    private String targetKey;

    private String targetParamsJson;

    private String targetOpenMode;

    private String dataJson;

    private LocalDateTime expireTime;

    private NoticePriority priority;

    private NoticeReadStatus readStatus;

    private LocalDateTime readTime;

    private NoticeDeleteStatus deleteStatus;

    private Boolean revokeStatus;

    private Boolean topStatus;

    private String bizType;

    private String bizId;

}
