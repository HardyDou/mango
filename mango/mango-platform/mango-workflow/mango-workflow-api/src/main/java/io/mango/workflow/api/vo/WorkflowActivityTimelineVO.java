package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowActivityKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例中的一行活动，来源为 Flowable 历史活动表。
 *
 * <p>排序规则：先按 {@code startTime} 升序，再按 {@code activityId} 升序；跨流程实例时调用方
 * 按 {@code processInstanceId} 分组后各自排序。网关行没有处理人，{@code assignee} 恒为 null。
 */
@Data
@Schema(description = "活动时间轴行")
public class WorkflowActivityTimelineVO {

    @Schema(description = "流程实例 ID")
    private String processInstanceId;

    @Schema(description = "活动定义 ID（BPMN 中的节点 id）")
    private String activityId;

    @Schema(description = "活动名称，BPMN 未指定名称时为空")
    private String activityName;

    @Schema(description = "Flowable 活动类型原值，如 exclusiveGateway、userTask、serviceTask")
    private String activityType;

    @Schema(description = "活动类别，便于调用方只取网关行或只取任务行")
    private WorkflowActivityKind kind;

    @Schema(description = "处理人标识；网关、事件等非人工活动为空")
    private String assignee;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间；仍在进行中的活动为空")
    private LocalDateTime endTime;

    @Schema(description = "活动耗时（毫秒）；未结束时为空")
    private Long durationMillis;
}
