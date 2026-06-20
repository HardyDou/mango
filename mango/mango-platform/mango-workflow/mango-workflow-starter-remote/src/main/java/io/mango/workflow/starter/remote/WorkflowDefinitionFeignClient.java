package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 流程定义远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowDefinitionFeignClient", path = "/workflow/definitions")
public interface WorkflowDefinitionFeignClient extends WorkflowDefinitionApi {

    @Override
    @PostMapping("/internal/ensure-published")
    R<WorkflowDeployVO> ensurePublished(@RequestBody EnsureWorkflowDefinitionCommand command);
}
