package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeRecipientTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 通知接收目标命令。
 */
@Data
@Schema(description = "通知接收目标命令")
public class NoticeRecipientTargetCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "接收目标类型不能为空")
    @Schema(description = "目标类型：USER、ORG、POST、ROLE")
    private NoticeRecipientTargetType targetType;

    @NotNull(message = "接收目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "目标名称，仅用于请求快照和页面回显")
    private String targetName;
}
