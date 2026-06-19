package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.query.WorkflowBusinessApplyProgressBatchQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    R<PageResult<WorkflowBusinessApplyVO>> page(@RequestBody(required = false) WorkflowBusinessApplyPageQuery query);

    @Override
    @GetMapping("/{applyId}")
    R<WorkflowBusinessApplyVO> detail(@PathVariable Long applyId);

    @Override
    @GetMapping("/history")
    R<PageResult<WorkflowBusinessApplyVO>> history(@RequestParam String businessType,
                                                   @RequestParam String businessKey,
                                                   @SpringQueryMap WorkflowBusinessApplyPageQuery query);

    @Override
    @GetMapping("/progress/latest")
    R<WorkflowBusinessApplyProgressVO> latestProgress(@RequestParam String businessType,
                                                      @RequestParam String businessKey);

    @Override
    default Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType,
                                                                        Collection<String> businessKeys) {
        WorkflowBusinessApplyProgressBatchQuery query = new WorkflowBusinessApplyProgressBatchQuery();
        query.setBusinessType(businessType);
        query.setBusinessKeys(businessKeys == null ? List.of() : List.copyOf(businessKeys));
        R<Map<String, WorkflowBusinessApplyProgressVO>> response = latestProgressBatch(query);
        return response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : Map.of();
    }

    @PostMapping("/progress/latest-batch")
    R<Map<String, WorkflowBusinessApplyProgressVO>> latestProgressBatch(
            @RequestBody WorkflowBusinessApplyProgressBatchQuery query);

    @Override
    default List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys) {
        return latestProgress(businessType, businessKeys).values().stream()
                .map(this::fromProgress)
                .toList();
    }

    private WorkflowBusinessApplyVO fromProgress(WorkflowBusinessApplyProgressVO progress) {
        WorkflowBusinessApplyVO vo = new WorkflowBusinessApplyVO();
        vo.setId(progress.getApplyId());
        vo.setApplyCode(progress.getApplyCode());
        vo.setBusinessType(progress.getBusinessType());
        vo.setBusinessKey(progress.getBusinessKey());
        vo.setApplyTitle(progress.getApplyTitle());
        vo.setProcessInstanceId(progress.getProcessInstanceId());
        vo.setProcessName(progress.getProcessName());
        vo.setApplyStatus(progress.getApplyStatus());
        vo.setApplyStatusName(progress.getApplyStatusName());
        vo.setCurrentTaskNames(progress.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(progress.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(progress.getCurrentAssigneeNames());
        vo.setCurrentTasks(progress.getCurrentTasks());
        vo.setCreatedAt(progress.getCreatedAt());
        vo.setUpdatedAt(progress.getUpdatedAt());
        return vo;
    }
}
