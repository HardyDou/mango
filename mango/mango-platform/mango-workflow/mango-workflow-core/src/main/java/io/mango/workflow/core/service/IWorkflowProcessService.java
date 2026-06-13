package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;

/**
 * 流程实例服务。
 */
public interface IWorkflowProcessService extends WorkflowProcessApi {

    @Override
    R<WorkflowProcessInstanceVO> start(StartWorkflowProcessCommand command);

    R<PageResult<WorkflowProcessInstanceVO>> initiated(WorkflowTaskPageQuery query);

    R<WorkflowProcessDetailVO> detail(String processInstanceId);

    R<PageResult<WorkflowProcessInstanceVO>> historyByBusinessKey(String businessKey, WorkflowTaskPageQuery query);
}
