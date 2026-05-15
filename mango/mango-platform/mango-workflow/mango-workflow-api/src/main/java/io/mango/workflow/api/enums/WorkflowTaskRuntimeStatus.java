package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流任务运行态列表状态。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskRuntimeStatus {

    TODO("待办"),
    DONE("已办");

    private final String label;
}
