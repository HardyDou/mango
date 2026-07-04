package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "系统消息交互动作")
public class NoticeSiteMessageActionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "动作编码不能为空")
    @Schema(description = "动作编码")
    private String actionCode;

    @NotBlank(message = "动作名称不能为空")
    @Schema(description = "动作展示名称")
    private String actionLabel;

    @Schema(description = "交互类型")
    private NoticeSiteMessageActionInteractionType interactionType = NoticeSiteMessageActionInteractionType.EVENT;

    @Schema(description = "动作事件类型，EVENT 动作必填")
    private String eventType;

    @Valid
    @Schema(description = "ROUTE 动作的跳转目标")
    private NoticeSiteMessageTargetCommand target;

    @Schema(description = "是否需要确认")
    private Boolean confirmRequired = false;

    @Schema(description = "输入 JSON Schema")
    private String inputSchema;

    @Schema(description = "排序")
    private Integer sortOrder = 0;

    @Schema(description = "动作过期时间")
    private LocalDateTime expireTime;
}
