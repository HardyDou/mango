package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_announcement_target")
public class NoticeAnnouncementTargetEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long announcementId;

    private NoticeAnnouncementTargetType targetType;

    private Long targetId;

    private String targetName;

    private Boolean includeChildren;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
