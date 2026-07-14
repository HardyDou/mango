package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeReadStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_announcement_recipient")
public class NoticeAnnouncementRecipientEntity extends NoticeBaseEntity {

    private Long announcementId;

    private Long userId;

    private NoticeReadStatus readStatus;

    private LocalDateTime readTime;

    private NoticeAnnouncementConfirmStatus confirmStatus;

    private LocalDateTime confirmTime;

}
