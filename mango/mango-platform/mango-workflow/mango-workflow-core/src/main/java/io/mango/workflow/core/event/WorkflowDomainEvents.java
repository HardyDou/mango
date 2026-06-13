package io.mango.workflow.core.event;

import io.mango.workflow.api.WorkflowEventTypes;

/**
 * 工作流领域事件类型。
 */
public final class WorkflowDomainEvents {

    public static final String PROCESS_STARTED = WorkflowEventTypes.PROCESS_STARTED;
    public static final String TASK_COMPLETED = WorkflowEventTypes.TASK_COMPLETED;
    public static final String TASK_REJECTED = WorkflowEventTypes.TASK_REJECTED;
    public static final String PROCESS_COMPLETED = WorkflowEventTypes.PROCESS_COMPLETED;
    public static final String PROCESS_REJECTED = WorkflowEventTypes.PROCESS_REJECTED;
    public static final String PROCESS_ENDED = WorkflowEventTypes.PROCESS_ENDED;

    private WorkflowDomainEvents() {
    }
}
