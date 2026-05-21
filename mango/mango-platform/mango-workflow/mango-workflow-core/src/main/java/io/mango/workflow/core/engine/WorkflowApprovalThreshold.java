package io.mango.workflow.core.engine;

import org.springframework.stereotype.Component;

/**
 * Flowable 多实例审批阈值计算工具。
 */
@Component("mangoWorkflowApprovalThreshold")
public class WorkflowApprovalThreshold {

    public int requiredApprovals(int totalInstances, int passRatio) {
        if (totalInstances <= 0) {
            return 1;
        }
        int ratio = Math.max(1, Math.min(100, passRatio));
        return Math.max(1, (int) Math.ceil(totalInstances * ratio / 100.0d));
    }
}
