package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyPageRequest;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 业务工作流申请远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowBusinessApplyFeignClient",
        path = "/workflow/business-applies")
public interface WorkflowBusinessApplyFeignClient extends WorkflowBusinessApplyApi {

    @Override
    @PostMapping
    R<WorkflowBusinessApplyVO> create(@RequestBody CreateWorkflowBusinessApplyCommand command);

    @Override
    @PostMapping("/page")
    R<PageResult<WorkflowBusinessApplyVO>> page(
            @RequestBody WorkflowBusinessApplyPageRequest request);

    @Override
    @GetMapping("/my/summary")
    R<WorkflowBusinessApplySummaryVO> mySummary();

    @Override
    @GetMapping("/detail")
    R<WorkflowBusinessApplyVO> detail(@RequestParam("applyId") Long applyId);

    @Override
    @GetMapping("/history")
    R<PageResult<WorkflowBusinessApplyVO>> history(@SpringQueryMap WorkflowBusinessApplyPageQuery query);

    @Override
    @GetMapping("/progress/latest")
    R<WorkflowBusinessApplyProgressVO> latestProgress(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessKey") String businessKey);

    @Override
    @PostMapping("/progress/latest-batch")
    R<WorkflowBusinessApplyProgressBatchVO> latestProgressBatch(
            @RequestBody WorkflowBusinessApplyProgressBatchRequest request);

    @Override
    @PostMapping("/latest-by-business-keys")
    R<List<WorkflowBusinessApplyVO>> latestByBusinessKeys(
            @RequestBody WorkflowBusinessApplyProgressBatchRequest request);

    @Override
    @GetMapping("/progress/by-process-instance")
    R<WorkflowBusinessApplyVO> byProcessInstance(
            @RequestParam("processInstanceId") String processInstanceId);
}
