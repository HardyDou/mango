package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务终止原因。
 *
 * <p>仅当任务状态为 {@code CANCELED} 时有值，供调用方做机器判断；中文文案由调用方自行渲染，
 * 平台不把引擎 {@code DELETE_REASON_} 的内部文案作为契约。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskCancelCause {

    /**
     * 被退回终止：该任务上存在退回动作记录。
     */
    RETURNED("被退回"),

    /**
     * 申请撤回导致终止：所在申请单状态为已撤回。
     */
    WITHDRAWN("申请撤回"),

    /**
     * 流程终止导致终止：所在申请单状态为已终止或已取消。
     */
    TERMINATED("流程终止"),

    /**
     * 其他原因，例如审批驳回导致流程实例被删除。
     */
    OTHER("其他");

    private final String label;
}
