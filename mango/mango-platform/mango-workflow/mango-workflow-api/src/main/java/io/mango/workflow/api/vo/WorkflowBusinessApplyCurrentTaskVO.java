package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务工作流申请当前任务视图。
 */
@Data
@Schema(description = "业务工作流申请当前任务视图")
public class WorkflowBusinessApplyCurrentTaskVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务定义Key")
    private String taskDefinitionKey;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "处理人ID")
    private Long assigneeId;

    @Schema(description = "处理人名称")
    private String assigneeName;

    @Schema(description = "到达时间")
    private LocalDateTime arrivedAt;
}
