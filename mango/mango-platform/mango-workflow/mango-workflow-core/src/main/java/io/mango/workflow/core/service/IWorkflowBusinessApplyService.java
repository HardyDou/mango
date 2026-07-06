package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务工作流申请中心服务。
 */
public interface IWorkflowBusinessApplyService {

    R<WorkflowBusinessApplyVO> create(CreateWorkflowBusinessApplyCommand command);

    R<PageResult<WorkflowBusinessApplyVO>> page(WorkflowBusinessApplyPageQuery query);

    R<WorkflowBusinessApplySummaryVO> mySummary();

    R<WorkflowBusinessApplyVO> detail(Long applyId);

    R<PageResult<WorkflowBusinessApplyVO>> history(String businessType, String businessKey,
                                                   WorkflowBusinessApplyPageQuery query);

    R<WorkflowBusinessApplyProgressVO> latestProgress(String businessType, String businessKey);

    Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType, Collection<String> businessKeys);

    List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys);

    void markProcessStarted(Long applyId, Long processDefinitionId, String processDefinitionKey,
                            String engineProcessDefinitionId, String processName, String processInstanceId);

    R<WorkflowBusinessApplyVO> byProcessInstance(String processInstanceId);

    WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId);

    void refreshCurrentTasks(String processInstanceId);

    WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId);

    void markApproved(String processInstanceId);

    void markRejected(String processInstanceId, String comment, String taskId, String taskDefinitionKey);

    void markTerminated(String processInstanceId, String comment, String taskId, String taskDefinitionKey);
}
