package io.mango.workflow.core.model;

import io.mango.workflow.api.enums.WorkflowParticipantType;
import lombok.Value;

/** 工作流参与事实写入参数。 */
@Value
public class WorkflowParticipantRecord {
    String processKey;
    String businessKey;
    String processInstanceId;
    Long userId;
    Long memberId;
    String username;
    String displayName;
    WorkflowParticipantType type;
}
