package io.mango.biz.notification.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.mango.biz.notification.api.enums.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class SysNotification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private NotificationType notificationType;
    private String title;
    private String content;
    private Long userId;
    private Integer priority;
    private Integer readStatus;
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
