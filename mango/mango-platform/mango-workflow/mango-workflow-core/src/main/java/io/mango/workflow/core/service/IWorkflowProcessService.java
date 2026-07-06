package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;

import java.util.Collection;
import java.util.List;

/**
 * 流程实例服务。
 */
public interface IWorkflowProcessService {

    R<WorkflowProcessInstanceVO> start(StartWorkflowProcessCommand command);

    R<WorkflowStartResultVO> startBusinessWorkflow(StartBusinessWorkflowCommand command);

    R<PageResult<WorkflowProcessInstanceVO>> initiated(WorkflowTaskPageQuery query);

    R<WorkflowProcessDetailVO> detail(String processInstanceId);

    R<PageResult<WorkflowProcessInstanceVO>> historyByBusinessKey(String businessKey, WorkflowTaskPageQuery query);

    List<WorkflowBusinessProcessVO> latestByBusinessKeys(Collection<String> businessKeys);

    List<WorkflowBusinessProcessVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys);
}
