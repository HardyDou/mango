package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;

/**
 * 工作流任务运行时服务。
 */
public interface IWorkflowTaskRuntimeService {

    R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query);

    R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query);

    R<WorkflowTaskDetailVO> detail(String taskId);

    R<Boolean> complete(CompleteWorkflowTaskCommand command);

    R<Boolean> reject(RejectWorkflowTaskCommand command);

    R<WorkflowProcessDetailVO> processDetail(String processInstanceId);

    void advanceRuntimeTasks(String processInstanceId);
}
