package io.mango.workflow.api.enums;

/**
 * 节点亲和允许复用办理人的最远作用域；运行时按由近及远逐层尝试，命中即停止。
 */
public enum WorkflowNodeAffinityScope {
    /** 只在当前流程实例内复用，与 1.7.0 行为一致。 */
    PROCESS_INSTANCE,
    /** 先当前实例，再同一业务键的历史实例。 */
    BUSINESS_KEY,
    /** 先当前实例，再同一业务键，最后同一亲和键的全部历史实例。 */
    AFFINITY_KEY
}
