package io.mango.biz.notification.api.vo;

import io.mango.biz.notification.api.enums.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysNotificationVO {
    private Long id;
    private NotificationType notificationType;
    private String title;
    private String content;
    private Long userId;
    private String username;
    private Integer priority;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
