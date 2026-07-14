package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.api.query.WorkflowBusinessKeysQuery;
import io.mango.workflow.api.query.WorkflowBusinessTypeKeysQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

/**
 * 业务侧流程状态远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowBusinessProcessFeignClient",
        path = "/workflow/processes")
public interface WorkflowBusinessProcessFeignClient extends WorkflowBusinessProcessApi {

    @Override
    @GetMapping("/business/latest-by-keys")
    R<List<WorkflowBusinessProcessVO>> latestByBusinessKeys(
            @SpringQueryMap WorkflowBusinessKeysQuery query);

    @Override
    @GetMapping("/business/latest-by-type-keys")
    R<List<WorkflowBusinessProcessVO>> latestByBusinessTypeKeys(
            @SpringQueryMap WorkflowBusinessTypeKeysQuery query);
}
