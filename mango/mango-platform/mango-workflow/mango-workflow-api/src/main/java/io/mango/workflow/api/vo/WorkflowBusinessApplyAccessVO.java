package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Workflow 业务申请数据权限校验上下文。
 */
@Getter
@AllArgsConstructor
@Schema(description = "Workflow 业务申请数据权限校验上下文")
public class WorkflowBusinessApplyAccessVO {

    @Schema(description = "申请 ID")
    private final Long applyId;

    @Schema(description = "流程实例 ID")
    private final String processInstanceId;

    @Schema(description = "业务类型")
    private final String businessType;

    @Schema(description = "业务主键")
    private final String businessKey;

    @Schema(description = "租户标识")
    private final String tenantId;

    @Schema(description = "申请所属组织 ID")
    private final Long orgId;

    @Schema(description = "申请人 ID")
    private final Long applicantId;
}
