package io.mango.workflow.api;

/**
 * 工作流领域事件类型。
 */
public final class WorkflowEventTypes {

    public static final String PROCESS_STARTED = "workflow.process.started";
    public static final String TASK_COMPLETED = "workflow.task.completed";
    public static final String TASK_ADVANCED = "workflow.task.advanced";
    public static final String TASK_REJECTED = "workflow.task.rejected";
    public static final String PROCESS_COMPLETED = "workflow.process.completed";
    public static final String PROCESS_REJECTED = "workflow.process.rejected";
    public static final String PROCESS_ENDED = "workflow.process.ended";

    private WorkflowEventTypes() {
    }
}
