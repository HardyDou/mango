package io.mango.workflow.core.engine;

import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import org.springframework.stereotype.Component;

/**
 * HTTP URL 节点执行器。
 */
@Component
public class HttpUrlWorkflowNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String executionType() {
        return "HTTP_URL";
    }

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        Require.fail(WorkflowCode.DESIGNER_INVALID.getCode(), "HTTP URL 节点执行器尚未配置允许访问的服务边界");
    }
}
