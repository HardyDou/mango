package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowParticipantType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 当前用户参与过的业务坐标。 */
@Data
@Schema(description = "工作流参与业务")
public class WorkflowParticipationBusinessVO {
    private String processKey;
    private String businessKey;
    private String processInstanceId;
    private List<WorkflowParticipantType> participantTypes = List.of();
    private LocalDateTime lastParticipatedAt;
}
