package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批中心任务视图。
 */
@Data
@Schema(description = "审批中心任务视图")
public class WorkflowTaskVO {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务定义Key，对应流程设计器节点定义Key")
    private String taskDefinitionKey;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程编码")
    private String processKey;

    @Schema(description = "Flowable流程定义ID")
    private String processDefinitionId;

    @Schema(description = "发起人")
    private String initiatorName;

    @Schema(description = "办理人")
    private String assigneeName;

    @Schema(description = "当前用户是否可认领")
    private Boolean claimable;

    @Schema(description = "当前用户是否可释放")
    private Boolean unclaimable;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
