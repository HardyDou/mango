package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessParticipantsVO;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.api.vo.WorkflowParticipationBusinessVO;
import jakarta.validation.Valid;

/** 工作流历史参与关系公开 API。 */
public interface WorkflowParticipationApi {
    R<WorkflowParticipationAccessVO> access(@Valid WorkflowParticipationAccessQuery query);

    R<PageResult<WorkflowParticipationBusinessVO>> my(@Valid WorkflowParticipationPageQuery query);

    R<WorkflowBusinessParticipantsVO> replaceBusinessParticipants(
            @Valid ReplaceWorkflowBusinessParticipantsCommand command);
}
