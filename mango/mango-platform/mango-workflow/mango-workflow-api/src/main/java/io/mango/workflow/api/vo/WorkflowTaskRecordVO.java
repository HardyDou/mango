package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流任务处理记录视图。
 */
@Data
@Schema(description = "工作流任务处理记录视图")
public class WorkflowTaskRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务定义Key")
    private String taskDefinitionKey;

    @Schema(description = "动作编码")
    private String action;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "处理人ID")
    private Long operatorId;

    @Schema(description = "处理人")
    private String operatorName;

    @Schema(description = "处理意见")
    private String comment;

    @Schema(description = "处理变量")
    private Map<String, Object> variables;

    @Schema(description = "处理时间")
    private LocalDateTime createdTime;
}
