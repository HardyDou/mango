package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowParticipationApi;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessParticipantsVO;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.api.vo.WorkflowParticipationBusinessVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 工作流参与关系远程客户端。 */
@FeignClient(name = "mango-workflow", contextId = "workflowParticipationFeignClient",
        path = "/workflow/participations")
public interface WorkflowParticipationFeignClient extends WorkflowParticipationApi {
    @Override
    @GetMapping("/access")
    R<WorkflowParticipationAccessVO> access(@SpringQueryMap WorkflowParticipationAccessQuery query);

    @Override
    @GetMapping("/my")
    R<PageResult<WorkflowParticipationBusinessVO>> my(@SpringQueryMap WorkflowParticipationPageQuery query);

    @Override
    @PostMapping("/business")
    R<WorkflowBusinessParticipantsVO> replaceBusinessParticipants(
            @RequestBody ReplaceWorkflowBusinessParticipantsCommand command);
}
