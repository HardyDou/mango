package io.mango.workflow.core.engine;

import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import org.springframework.stereotype.Component;

/**
 * 远程服务节点执行器。
 */
@Component
public class RemoteServiceWorkflowNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String executionType() {
        return "REMOTE_SERVICE";
    }

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        Require.fail(WorkflowCode.DESIGNER_INVALID.getCode(), "远程服务节点执行器尚未配置服务注册与调用边界");
    }
}
