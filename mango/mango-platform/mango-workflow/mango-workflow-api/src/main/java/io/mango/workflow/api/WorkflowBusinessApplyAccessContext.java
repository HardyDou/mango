package io.mango.workflow.api;

/**
 * Workflow 业务申请数据权限校验上下文。
 *
 * @param applyId 申请 ID。
 * @param processInstanceId 流程实例 ID。
 * @param businessType 业务类型。
 * @param businessKey 业务主键。
 * @param tenantId 租户标识。
 * @param orgId 申请所属组织 ID。
 * @param applicantId 申请人 ID，可作为默认 owner 事实。
 */
public record WorkflowBusinessApplyAccessContext(
        Long applyId,
        String processInstanceId,
        String businessType,
        String businessKey,
        String tenantId,
        Long orgId,
        Long applicantId
) {
}
