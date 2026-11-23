package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务终止原因。
 *
 * <p>{@code CANCELED} 任务恒有值；{@code DONE} 任务在「该任务上存在驳回/退回处理记录」时也有值
 * （{@code REJECTED} / {@code RETURNED}），其余状态为 {@code null}。中文文案由调用方自行渲染，
 * 平台不把引擎 {@code DELETE_REASON_} 的内部文案作为契约。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskCancelCause {

    /**
     * 被退回结束：该任务上存在退回动作记录。
     *
     * <p>{@code DONE} 表示本人执行了退回；{@code CANCELED} 表示该任务被退回动作连带终止。
     */
    RETURNED("被退回"),

    /**
     * 被驳回结束：该任务上存在驳回或自动驳回记录，或所在申请单已被驳回。
     *
     * <p>{@code DONE} 表示本人驳回了申请；{@code CANCELED} 表示申请被驳回时该任务仍未办理、
     * 随流程实例一起被删除。
     */
    REJECTED("被驳回"),

    /**
     * 申请撤回导致终止：所在申请单状态为已撤回。
     */
    WITHDRAWN("申请撤回"),

    /**
     * 流程终止导致终止：所在申请单状态为已终止或已取消。
     */
    TERMINATED("流程终止"),

    /**
     * 其他原因：既没有任务级处理记录，申请单状态也不在已知终止范围内。
     */
    OTHER("其他");

    private final String label;
}
