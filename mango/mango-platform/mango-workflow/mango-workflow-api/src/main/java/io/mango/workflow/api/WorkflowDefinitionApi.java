package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.vo.WorkflowDeployVO;

/**
 * 流程定义 API。
 */
public interface WorkflowDefinitionApi {

    /**
     * 确保流程定义存在并处于已发布可发起状态。
     *
     * @param command 流程定义初始化命令
     * @return 流程发布结果
     */
    R<WorkflowDeployVO> ensurePublished(EnsureWorkflowDefinitionCommand command);
}
