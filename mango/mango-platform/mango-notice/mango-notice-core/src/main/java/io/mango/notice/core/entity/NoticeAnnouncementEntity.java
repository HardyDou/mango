package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_announcement")
public class NoticeAnnouncementEntity extends NoticeBaseEntity {

    private String title;

    private String content;

    private NoticeAnnouncementStatus status;

    private LocalDateTime publishTime;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private Boolean pinned;

    private Boolean confirmRequired;

    private Boolean syncMessageEnabled;

}
