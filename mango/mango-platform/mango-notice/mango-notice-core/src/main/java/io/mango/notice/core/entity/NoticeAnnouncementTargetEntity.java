package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_announcement_target")
public class NoticeAnnouncementTargetEntity extends NoticeBaseEntity {

    private Long announcementId;

    private NoticeAnnouncementTargetType targetType;

    private Long targetId;

    private String targetName;

    private Boolean includeChildren;

}
