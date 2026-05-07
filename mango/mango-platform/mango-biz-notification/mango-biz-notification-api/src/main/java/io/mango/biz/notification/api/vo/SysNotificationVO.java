package io.mango.biz.notification.api.vo;

import io.mango.biz.notification.api.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "消息通知视图")
public class SysNotificationVO {
    @Schema(description = "消息ID")
    private Long id;
    @Schema(description = "通知类型")
    private NotificationType notificationType;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "接收用户ID")
    private Long userId;
    @Schema(description = "接收用户名")
    private String username;
    @Schema(description = "优先级")
    private Integer priority;
    @Schema(description = "已读状态：0-未读，1-已读")
    private Integer readStatus;
    @Schema(description = "已读时间")
    private LocalDateTime readTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
