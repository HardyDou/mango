package io.mango.workflow.core.service;

import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessParticipantsVO;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.api.vo.WorkflowParticipationBusinessVO;
import io.mango.workflow.core.model.WorkflowParticipantRecord;

import java.util.Collection;

/** 工作流参与关系服务。 */
public interface IWorkflowParticipationService {
    WorkflowParticipationAccessVO access(WorkflowParticipationAccessQuery query);

    PageResult<WorkflowParticipationBusinessVO> my(WorkflowParticipationPageQuery query);

    WorkflowBusinessParticipantsVO replaceBusinessParticipants(
            ReplaceWorkflowBusinessParticipantsCommand command);

    void recordParticipant(WorkflowParticipantRecord record);

    void deactivateCurrentAssignee(String processInstanceId, Long userId);

    void deactivateCurrentAssignees(String processInstanceId);
}
