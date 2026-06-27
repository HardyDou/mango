package io.mango.workflow.core.service;

import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;

/**
 * Result produced after workflow runtime tasks are advanced and business current tasks are refreshed.
 */
public record WorkflowTaskAdvanceResult(
        String processInstanceId,
        boolean ended,
        WorkflowBusinessApplyVO businessApply
) {
}
