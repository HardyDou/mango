package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.model.WorkflowProcessStartedContext;
import io.mango.workflow.core.model.WorkflowTaskStatusContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务工作流申请中心服务。
 */
public interface IWorkflowBusinessApplyService {

    WorkflowBusinessApplyVO create(CreateWorkflowBusinessApplyCommand command);

    PageResult<WorkflowBusinessApplyVO> page(WorkflowBusinessApplyPageQuery query);

    WorkflowBusinessApplySummaryVO mySummary();

    WorkflowBusinessApplyVO detail(Long applyId);

    PageResult<WorkflowBusinessApplyVO> history(WorkflowBusinessApplyPageQuery query);

    WorkflowBusinessApplyProgressVO latestProgress(String businessType, String businessKey);

    Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType, Collection<String> businessKeys);

    WorkflowBusinessApplyProgressBatchVO latestProgressBatch(WorkflowBusinessApplyProgressBatchRequest request);

    List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys);

    void markProcessStarted(WorkflowProcessStartedContext context);

    WorkflowBusinessApplyVO byProcessInstance(String processInstanceId);

    WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId);

    WorkflowBusinessApplyVO lockWithdrawalTarget(Long applyId, String processInstanceId);

    void refreshCurrentTasks(String processInstanceId);

    WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId);

    void markApproved(String processInstanceId);

    void markRejected(WorkflowTaskStatusContext context);

    void markTerminated(WorkflowTaskStatusContext context);

    WorkflowBusinessApplyVO markWithdrawn(String processInstanceId, String reason);
}
