package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流任务认领状态。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskClaimStatus {

    NONE("无当前任务"),
    UNCLAIMED("待领取"),
    ASSIGNED("已分配");

    private final String label;
}
