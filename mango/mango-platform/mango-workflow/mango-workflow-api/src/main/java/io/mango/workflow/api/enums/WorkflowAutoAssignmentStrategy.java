package io.mango.workflow.api.enums;

/** 自动派单选择候选人的策略。 */
public enum WorkflowAutoAssignmentStrategy {
    /** 按租户内稳定用户 ID 轮询。 */
    ROUND_ROBIN,
    /** 选择当前活动任务数量最少的候选人。 */
    LEAST_TASKS,
    /** 优先选择同一流程实例最近处理过任务的候选人。 */
    AFFINITY
}
