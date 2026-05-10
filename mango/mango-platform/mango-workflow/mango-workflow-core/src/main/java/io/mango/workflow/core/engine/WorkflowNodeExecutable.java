package io.mango.workflow.core.engine;

/**
 * 白名单 Spring Bean 工作流节点执行接口。
 */
public interface WorkflowNodeExecutable {

    /**
     * 执行节点动作。
     *
     * @param context 节点执行上下文。
     */
    void execute(WorkflowNodeExecutionContext context);
}
