package io.mango.workflow.starter.remote;

import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * 业务侧流程状态远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowBusinessProcessFeignClient",
        path = "/workflow/processes")
public interface WorkflowBusinessProcessFeignClient extends WorkflowBusinessProcessApi {

    @Override
    @GetMapping("/business/latest-by-keys")
    List<WorkflowBusinessProcessVO> latestByBusinessKeys(
            @RequestParam("businessKeys") Collection<String> businessKeys);

    @Override
    @GetMapping("/business/latest-by-type-keys")
    List<WorkflowBusinessProcessVO> latestByBusinessKeys(@RequestParam("businessType") String businessType,
                                                         @RequestParam("businessKeys") Collection<String> businessKeys);
}
