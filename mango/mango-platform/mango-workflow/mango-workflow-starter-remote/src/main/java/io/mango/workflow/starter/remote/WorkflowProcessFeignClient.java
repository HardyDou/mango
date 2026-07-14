package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 流程实例远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowProcessFeignClient", path = "/workflow/processes")
public interface WorkflowProcessFeignClient extends WorkflowProcessApi {

    @Override
    @PostMapping("/start")
    R<WorkflowProcessInstanceVO> start(@RequestBody StartWorkflowProcessCommand command);

    @Override
    @PostMapping("/start-business")
    R<WorkflowStartResultVO> startBusinessWorkflow(@RequestBody StartBusinessWorkflowCommand command);

    @Override
    @GetMapping("/initiated")
    R<PageResult<WorkflowProcessInstanceVO>> initiated(@SpringQueryMap WorkflowTaskPageQuery query);

    @Override
    @GetMapping("/detail")
    R<WorkflowProcessDetailVO> detail(@RequestParam("processInstanceId") String processInstanceId);

    @Override
    @GetMapping("/history")
    R<PageResult<WorkflowProcessInstanceVO>> history(
            @RequestParam("businessKey") String businessKey,
            @SpringQueryMap WorkflowTaskPageQuery query);
}
