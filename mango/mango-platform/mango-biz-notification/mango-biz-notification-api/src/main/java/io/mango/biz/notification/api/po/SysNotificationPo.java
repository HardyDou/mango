package io.mango.biz.notification.api.po;

import io.mango.biz.notification.api.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "消息通知")
public class SysNotificationPo implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "通知类型")
    @NotNull(message = "notificationType不能为空")
    private NotificationType notificationType;

    @Schema(description = "标题")
    @NotBlank(message = "title不能为空")
    private String title;

    @Schema(description = "内容")
    @NotBlank(message = "content不能为空")
    private String content;

    @Schema(description = "接收用户ID列表")
    private List<Long> userIds;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "优先级")
    private Integer priority;
}
