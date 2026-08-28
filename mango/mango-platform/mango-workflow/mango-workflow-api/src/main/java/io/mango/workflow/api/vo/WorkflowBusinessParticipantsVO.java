package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 业务声明参与人结果。 */
@Data
@Schema(description = "工作流业务参与人结果")
public class WorkflowBusinessParticipantsVO {
    private String processKey;
    private String businessKey;
    private String processInstanceId;
    private List<Long> participantUserIds = List.of();
}
