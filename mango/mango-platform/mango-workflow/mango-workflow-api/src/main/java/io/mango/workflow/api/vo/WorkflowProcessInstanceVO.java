package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例视图。
 */
@Data
@Schema(description = "流程实例视图")
public class WorkflowProcessInstanceVO {

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "流程定义ID")
    private Long definitionId;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程编码")
    private String processKey;

    @Schema(description = "Flowable流程定义ID")
    private String processDefinitionId;

    @Schema(description = "发起人")
    private String initiatorName;

    @Schema(description = "当前执行节点名称")
    private String currentTaskName;

    @Schema(description = "当前执行节点定义键")
    private String currentTaskDefinitionKey;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "发起时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
