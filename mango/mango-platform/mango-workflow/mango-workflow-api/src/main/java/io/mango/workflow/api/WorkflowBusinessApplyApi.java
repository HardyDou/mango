package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyPageRequest;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 业务工作流申请中心 API。
 */
public interface WorkflowBusinessApplyApi {

    R<WorkflowBusinessApplyVO> create(@Valid CreateWorkflowBusinessApplyCommand command);

    R<PageResult<WorkflowBusinessApplyVO>> page(@Valid WorkflowBusinessApplyPageRequest request);

    R<WorkflowBusinessApplySummaryVO> mySummary();

    R<WorkflowBusinessApplyVO> detail(@NotNull Long applyId);

    R<PageResult<WorkflowBusinessApplyVO>> history(@Valid WorkflowBusinessApplyPageQuery query);

    R<WorkflowBusinessApplyProgressVO> latestProgress(
            @NotBlank String businessType, @NotBlank String businessKey);

    R<WorkflowBusinessApplyProgressBatchVO> latestProgressBatch(
            @Valid WorkflowBusinessApplyProgressBatchRequest request);

    R<List<WorkflowBusinessApplyVO>> latestByBusinessKeys(
            @Valid WorkflowBusinessApplyProgressBatchRequest request);

    R<WorkflowBusinessApplyVO> byProcessInstance(@NotBlank String processInstanceId);
}
