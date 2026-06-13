package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;

/**
 * 流程实例 API。
 */
public interface WorkflowProcessApi {

    /**
     * 发起流程。
     *
     * @param command 发起流程命令
     * @return 流程实例
     */
    R<WorkflowProcessInstanceVO> start(StartWorkflowProcessCommand command);
}
