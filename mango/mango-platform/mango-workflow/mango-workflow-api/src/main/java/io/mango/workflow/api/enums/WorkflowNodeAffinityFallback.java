package io.mango.workflow.api.enums;

/** 节点亲和没有历史办理人时使用的回退策略。 */
public enum WorkflowNodeAffinityFallback {
    /** 保持待领取，由候选人自行认领。 */
    CLAIM,
    /** 按租户内稳定用户 ID 轮询候选人。 */
    ROUND_ROBIN,
    /** 从当前候选人中随机选择。 */
    RANDOM,
    /** 选择当前活动任务数量最少的候选人。 */
    LEAST_TASKS
}
