package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 业务审批流程撤回结果。
 */
@Data
@Schema(description = "业务审批流程撤回结果")
public class WorkflowProcessWithdrawResultVO {

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "撤回前申请状态")
    private WorkflowApplyStatus previousStatus;

    @Schema(description = "当前申请状态")
    private WorkflowApplyStatus applyStatus;

    @Schema(description = "当前申请状态名称")
    private String applyStatusName;

    @Schema(description = "是否已撤回")
    private Boolean withdrawn;

    @Schema(description = "是否为重复撤回的幂等结果")
    private Boolean idempotent;

    @Schema(description = "流程是否已结束")
    private Boolean ended;

    @Schema(description = "撤回原因")
    private String reason;
}
