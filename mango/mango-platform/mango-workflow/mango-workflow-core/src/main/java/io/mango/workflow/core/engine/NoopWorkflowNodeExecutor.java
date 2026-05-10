package io.mango.workflow.core.engine;

import org.springframework.stereotype.Component;

/**
 * 无动作节点执行器。
 */
@Component
public class NoopWorkflowNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String executionType() {
        return "NONE";
    }

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        // 无动作节点只完成流程流转。
    }
}
