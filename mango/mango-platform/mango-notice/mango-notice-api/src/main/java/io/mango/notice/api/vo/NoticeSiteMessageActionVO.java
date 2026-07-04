package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统消息动作视图")
public class NoticeSiteMessageActionVO {

    @Schema(description = "动作 ID")
    private Long id;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "动作展示名称")
    private String actionLabel;

    @Schema(description = "交互类型")
    private NoticeSiteMessageActionInteractionType interactionType;

    @Schema(description = "动作事件类型")
    private String eventType;

    @Schema(description = "动作跳转目标")
    private NoticeSiteMessageTargetVO target;

    @Schema(description = "是否需要确认")
    private Boolean confirmRequired;

    @Schema(description = "输入 JSON Schema")
    private String inputSchema;

    @Schema(description = "动作状态")
    private NoticeSiteMessageActionStatus status;

    @Schema(description = "失败原因")
    private String failureReason;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "动作过期时间")
    private LocalDateTime expireTime;
}
