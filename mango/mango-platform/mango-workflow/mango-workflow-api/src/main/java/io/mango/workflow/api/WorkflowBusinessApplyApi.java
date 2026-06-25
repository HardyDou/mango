package io.mango.workflow.api;

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
 * 业务工作流申请中心 API。
 */
public interface WorkflowBusinessApplyApi {

    R<WorkflowBusinessApplyVO> create(CreateWorkflowBusinessApplyCommand command);

    R<PageResult<WorkflowBusinessApplyVO>> page(WorkflowBusinessApplyPageQuery query);

    R<WorkflowBusinessApplySummaryVO> mySummary();

    R<WorkflowBusinessApplyVO> detail(Long applyId);

    R<PageResult<WorkflowBusinessApplyVO>> history(String businessType, String businessKey, WorkflowBusinessApplyPageQuery query);

    R<WorkflowBusinessApplyProgressVO> latestProgress(String businessType, String businessKey);

    Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType, Collection<String> businessKeys);

    List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys);
}
