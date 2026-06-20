package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 流程实例远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowProcessFeignClient", path = "/workflow/processes")
public interface WorkflowProcessFeignClient extends WorkflowProcessApi {

    @Override
    @PostMapping("/start")
    R<WorkflowProcessInstanceVO> start(@RequestBody StartWorkflowProcessCommand command);
}
