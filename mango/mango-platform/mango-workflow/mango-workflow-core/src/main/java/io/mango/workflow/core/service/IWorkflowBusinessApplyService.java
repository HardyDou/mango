package io.mango.workflow.core.service;

import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;

/**
 * 业务工作流申请中心服务。
 */
public interface IWorkflowBusinessApplyService extends WorkflowBusinessApplyApi {

    void markProcessStarted(Long applyId, Long processDefinitionId, String processDefinitionKey,
                            String engineProcessDefinitionId, String processName, String processInstanceId);

    io.mango.common.result.R<WorkflowBusinessApplyVO> byProcessInstance(String processInstanceId);

    WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId);

    void refreshCurrentTasks(String processInstanceId);

    WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId);

    void markApproved(String processInstanceId);

    void markRejected(String processInstanceId, String comment, String taskId, String taskDefinitionKey);

    void markTerminated(String processInstanceId, String comment, String taskId, String taskDefinitionKey);
}
