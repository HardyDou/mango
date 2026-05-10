package io.mango.workflow.core.engine;

/**
 * 工作流节点执行器。
 */
public interface WorkflowNodeExecutor {

    /**
     * 执行类型。
     *
     * @return 执行类型编码。
     */
    String executionType();

    /**
     * 执行节点动作。
     *
     * @param context 节点执行上下文。
     */
    void execute(WorkflowNodeExecutionContext context);
}
