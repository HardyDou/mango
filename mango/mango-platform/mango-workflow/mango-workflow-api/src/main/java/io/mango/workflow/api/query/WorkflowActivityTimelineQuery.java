package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务键读取活动时间轴的条件。
 *
 * <p>与流转记录（{@code traceByBusinessKey}）的区别：流转记录以任务为粒度、带处理人与动作；
 * 活动时间轴以 {@code ACT_HI_ACTINST} 为粒度，包含网关、事件等非任务活动，用于还原完整路径。
 * 业务键可对应多个流程实例（重新申请），返回结果是这些实例活动的并集，调用方按
 * {@code processInstanceId} 分组。
 */
@Data
@Schema(description = "按业务键读取活动时间轴条件")
public class WorkflowActivityTimelineQuery {

    @Schema(description = "业务类型，按业务申请表的 business_type 精确匹配", example = "BAOHAN_RC_PROJECT_REVIEW")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型长度不能超过128")
    private String businessType;

    @Schema(description = "业务主键，按业务申请表的 business_key 精确匹配", example = "RC202611130001")
    @NotBlank(message = "业务主键不能为空")
    @Size(max = 128, message = "业务主键长度不能超过128")
    private String businessKey;

    @Schema(description = "申请人部门 ID 集合，可选；命中业务申请的申请人部门时整条流程实例参与返回，不传或为空表示不过滤")
    @Size(max = 500, message = "部门数量不能超过500")
    private List<Long> deptIds;

    @Schema(description = "活动开始时间起，闭区间；不传表示不限")
    private LocalDateTime beginTime;

    @Schema(description = "活动开始时间止，闭区间；不传表示不限")
    private LocalDateTime endTime;
}
