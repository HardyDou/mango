package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_announcement")
public class NoticeAnnouncementEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String content;

    private NoticeAnnouncementStatus status;

    private LocalDateTime publishTime;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private Boolean pinned;

    private Boolean confirmRequired;

    private Boolean syncMessageEnabled;

    private String tenantId;

    private Long orgId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
