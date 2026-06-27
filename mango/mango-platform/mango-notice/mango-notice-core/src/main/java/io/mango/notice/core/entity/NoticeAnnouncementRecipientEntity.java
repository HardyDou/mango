package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeReadStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_announcement_recipient")
public class NoticeAnnouncementRecipientEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long announcementId;

    private Long userId;

    private NoticeReadStatus readStatus;

    private LocalDateTime readTime;

    private NoticeAnnouncementConfirmStatus confirmStatus;

    private LocalDateTime confirmTime;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
