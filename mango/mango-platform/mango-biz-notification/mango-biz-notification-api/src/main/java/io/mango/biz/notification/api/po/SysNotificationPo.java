package io.mango.biz.notification.api.po;

import io.mango.biz.notification.api.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SysNotificationPo implements Serializable {

    private static final long serialVersionUID = 1L;
    @NotNull(message = "notificationType不能为空")
    private NotificationType notificationType;

    @NotBlank(message = "title不能为空")
    private String title;

    @NotBlank(message = "content不能为空")
    private String content;

    private List<Long> userIds;

    private Long userId;

    private Integer priority;
}
